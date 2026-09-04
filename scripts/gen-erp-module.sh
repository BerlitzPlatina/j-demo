#!/usr/bin/env bash
#
# Scaffold an ERP feature package modelled on com.example.erp.organization.
#
# Generates entity, four DTO records, mapper, DAO, service, controller and a
# liquibase changelog, then registers the changelog in changelog-master.yaml.
#
#   ./scripts/gen-erp-module.sh product name:string:notnull,unique,max=255 price:decimal:min=0
#   ./scripts/gen-erp-module.sh product          # no field given -> prompts for them
#
set -euo pipefail

# --------------------------------------------------------------------- usage

usage() {
    cat <<'USAGE'
Usage: gen-erp-module.sh [options] <feature> [field ...]

  <feature>  singular feature name: product | purchase-order | PurchaseOrder

  field      name:type[:constraint,...]
             type        string | text | int | long | decimal | bool
                         | date | datetime | ref(OtherEntity)
             constraint  notnull | unique | max=N | min=N

             max=N sets the VARCHAR length and @Size(max = N) for strings,
             or @Max(N) for numbers. min=N maps to @Min(N).

  When no field is passed, the script asks for them one per line and stops on
  an empty line. It refuses to guess a schema.

Options:
  --dry-run     print the files that would be written, write nothing
  --compile     run ./mvnw -q -pl erp -am compile afterwards
  --force       overwrite an existing feature package
  -h, --help    this text

Examples:
  gen-erp-module.sh product name:string:notnull,unique,max=255 \
      code:string:unique,max=50 description:text price:decimal:min=0 \
      active:bool:notnull category:'ref(Organization)':notnull
USAGE
}

DRY_RUN=0
COMPILE=0
FORCE=0
ARGS=()
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=1 ;;
        --compile) COMPILE=1 ;;
        --force) FORCE=1 ;;
        -h|--help) usage; exit 0 ;;
        -*) echo "unknown option: $arg" >&2; usage >&2; exit 2 ;;
        *) ARGS+=("$arg") ;;
    esac
done

ROOT=$(git -C "$(dirname "$0")" rev-parse --show-toplevel 2>/dev/null) \
    || ROOT=$(cd "$(dirname "$0")/.." && pwd)
JAVA_ROOT="$ROOT/erp/src/main/java/com/example/erp"
CHANGELOG_DIR="$ROOT/erp/src/main/resources/db/changelog"
MASTER="$CHANGELOG_DIR/changelog-master.yaml"
AUTHOR=${ERP_CHANGELOG_AUTHOR:-nam}

[ -d "$JAVA_ROOT" ] || { echo "not an erp checkout: $JAVA_ROOT missing" >&2; exit 1; }

# ----------------------------------------------------------------- name utils

to_snake() {
    printf '%s' "$1" | sed -E 's/([a-z0-9])([A-Z])/\1_\2/g; s/[-[:space:]]+/_/g; s/__+/_/g' \
        | tr '[:upper:]' '[:lower:]'
}

to_pascal() {
    to_snake "$1" | awk -F_ '{for (i = 1; i <= NF; i++) printf toupper(substr($i, 1, 1)) substr($i, 2)}'
}

to_camel() {
    local p
    p=$(to_pascal "$1")
    printf '%s%s' "$(printf '%s' "${p:0:1}" | tr '[:upper:]' '[:lower:]')" "${p:1}"
}

to_kebab() { to_snake "$1" | tr '_' '-'; }

# Naive English plural, enough for table and route names.
pluralize() {
    local s=$1
    case "$s" in
        *[^aeiou]y) printf '%sies' "${s%y}" ;;
        *s|*x|*z|*ch|*sh) printf '%ses' "$s" ;;
        *) printf '%ss' "$s" ;;
    esac
}

# ------------------------------------------------------------- feature naming

FEATURE=${ARGS[0]:-}
if [ -z "$FEATURE" ]; then
    if [ -t 0 ]; then
        read -r -p "Feature name (singular, e.g. product): " FEATURE
    fi
    [ -n "$FEATURE" ] || { echo "a feature name is required" >&2; exit 2; }
fi
ARGS=("${ARGS[@]:1}")

ENTITY=$(to_pascal "$FEATURE")
SNAKE=$(to_snake "$FEATURE")
PKG=$(printf '%s' "$SNAKE" | tr -d '_')
CAMEL=$(to_camel "$FEATURE")
TABLE=$(pluralize "$SNAKE")
ROUTE="/api/$(pluralize "$(to_kebab "$FEATURE")")"

case "$ENTITY" in
    [A-Z]*) : ;;
    *) echo "feature name must start with a letter: $FEATURE" >&2; exit 2 ;;
esac

# ------------------------------------------------------------- field parsing

F_NAME=(); F_PASCAL=(); F_COL=(); F_KIND=(); F_JAVA=(); F_SQL=()
F_NOTNULL=(); F_UNIQUE=(); F_MAX=(); F_MIN=(); F_REF=()

parse_field() {
    local raw=$1 name rest kind constr ref='' max='' min='' notnull=0 unique=0 java sql

    case "$raw" in
        *:*) : ;;
        *) echo "field '$raw' needs a type: name:type[:constraints]" >&2; return 1 ;;
    esac
    name=${raw%%:*}
    rest=${raw#*:}
    kind=${rest%%:*}
    constr=''
    [ "$rest" = "$kind" ] || constr=${rest#*:}

    case "$kind" in
        ref\(*\)) ref=$(to_pascal "${kind#ref(}"); ref=${ref%\)}; kind=ref ;;
    esac
    # to_pascal already dropped the ')' for the ref case above; guard anyway.
    ref=${ref%)}

    local IFS=','
    for c in $constr; do
        case "$c" in
            '') : ;;
            notnull) notnull=1 ;;
            unique) unique=1 ;;
            max=*) max=${c#max=} ;;
            min=*) min=${c#min=} ;;
            *) echo "unknown constraint '$c' on field '$name'" >&2; return 1 ;;
        esac
    done
    unset IFS

    case "$kind" in
        string)   java=String;     sql="VARCHAR(${max:-255})" ;;
        text)     java=String;     sql=TEXT ;;
        int)      java=Integer;    sql=INT ;;
        long)     java=Long;       sql=BIGINT ;;
        decimal)  java=BigDecimal; sql='DECIMAL(19, 2)' ;;
        bool)     java=Boolean;    sql=BOOLEAN ;;
        date)     java=LocalDate;  sql=DATE ;;
        datetime) java=LocalDateTime; sql=DATETIME ;;
        ref)
            [ -n "$ref" ] || { echo "ref field '$name' needs a target: ref(Other)" >&2; return 1; }
            java=$ref; sql=BIGINT ;;
        *) echo "unknown type '$kind' on field '$name'" >&2; return 1 ;;
    esac

    F_NAME+=("$(to_camel "$name")")
    F_PASCAL+=("$(to_pascal "$name")")
    F_COL+=("$(to_snake "$name")")
    F_KIND+=("$kind")
    F_JAVA+=("$java")
    F_SQL+=("$sql")
    F_NOTNULL+=("$notnull")
    F_UNIQUE+=("$unique")
    F_MAX+=("$max")
    F_MIN+=("$min")
    F_REF+=("$ref")
}

if [ ${#ARGS[@]} -eq 0 ]; then
    if [ ! -t 0 ]; then
        echo "no field given and stdin is not a terminal; pass fields as arguments" >&2
        exit 2
    fi
    cat >&2 <<'PROMPT'
No field given, so enter them by hand - one per line, empty line to finish.

  format  name:type[:constraint,...]
  type    string text int long decimal bool date datetime ref(Other)
  cons.   notnull unique max=N min=N
  e.g.    name:string:notnull,unique,max=255
          price:decimal:min=0
          organization:ref(Organization):notnull

PROMPT
    while :; do
        read -r -p "field> " line || break
        [ -n "${line// /}" ] || break
        parse_field "$line" || true
    done
    [ ${#F_NAME[@]} -gt 0 ] || { echo "no field entered, nothing to generate" >&2; exit 2; }
else
    for a in "${ARGS[@]}"; do parse_field "$a"; done
fi

NFIELDS=${#F_NAME[@]}

# First string-ish field carries the keyword search; prefer one literally named "name".
KEYWORD_IDX=-1
for i in $(seq 0 $((NFIELDS - 1))); do
    case "${F_KIND[$i]}" in
        string|text)
            if [ "${F_NAME[$i]}" = "name" ]; then KEYWORD_IDX=$i; break; fi
            [ $KEYWORD_IDX -lt 0 ] && KEYWORD_IDX=$i ;;
    esac
done

# ------------------------------------------------------------------ emitting

BASE="$JAVA_ROOT/$PKG"
if [ -d "$BASE" ] && [ $FORCE -eq 0 ]; then
    echo "feature package already exists: ${BASE#$ROOT/} (use --force to overwrite)" >&2
    exit 1
fi

# Every generator pipes into `write`, which therefore runs in a subshell: the list of written
# files has to live in a file rather than in an array the subshell could not export back.
WRITTEN_LOG=$(mktemp)
trap 'rm -f "$WRITTEN_LOG"' EXIT

# write <relative path>; body on stdin
write() {
    local rel=$1 path="$ROOT/$1"
    if [ $DRY_RUN -eq 1 ]; then
        echo "--- $rel"
        cat
    else
        mkdir -p "$(dirname "$path")"
        cat > "$path"
    fi
    echo "$rel" >> "$WRITTEN_LOG"
}

IMPORTS=()
add_import() { IMPORTS+=("$1"); }
emit_imports() {
    local all nonjava javas
    all=$(printf '%s\n' "${IMPORTS[@]}" | sort -u)
    nonjava=$(printf '%s\n' "$all" | grep -v '^java\.' || true)
    javas=$(printf '%s\n' "$all" | grep '^java\.' || true)
    [ -n "$nonjava" ] && printf '%s\n' "$nonjava" | sed 's/^/import /; s/$/;/'
    if [ -n "$javas" ]; then
        [ -n "$nonjava" ] && echo
        printf '%s\n' "$javas" | sed 's/^/import /; s/$/;/'
    fi
}

# Value-type imports needed by a DTO or the entity.
add_type_imports() {
    local i
    for i in $(seq 0 $((NFIELDS - 1))); do
        case "${F_JAVA[$i]}" in
            BigDecimal) add_import java.math.BigDecimal ;;
            LocalDate) add_import java.time.LocalDate ;;
            LocalDateTime) add_import java.time.LocalDateTime ;;
        esac
    done
}

# ------------------------------------------------------------------- entity

gen_entity() {
    IMPORTS=()
    local i has_ref=0
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] && has_ref=1
    done

    add_import jakarta.persistence.Column
    add_import jakarta.persistence.Entity
    add_import jakarta.persistence.Table
    add_import com.example.common.jpa.entity.AbstractAuditModel
    add_import lombok.AllArgsConstructor
    add_import lombok.Builder
    add_import lombok.Data
    add_import lombok.EqualsAndHashCode
    add_import lombok.NoArgsConstructor
    add_import lombok.ToString
    if [ $has_ref -eq 1 ]; then
        add_import jakarta.persistence.FetchType
        add_import jakarta.persistence.JoinColumn
        add_import jakarta.persistence.ManyToOne
    fi
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] && [ "${F_REF[$i]}" != "$ENTITY" ] \
            && add_import "com.example.erp.$(printf '%s' "$(to_snake "${F_REF[$i]}")" | tr -d '_').entity.${F_REF[$i]}"
    done
    add_type_imports

    {
        echo "package com.example.erp.$PKG.entity;"
        echo
        emit_imports
        cat <<EOF

/**
 * The {@code $TABLE} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so they are not
 * repeated here.
 */
@Entity
@Table(name = "$TABLE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class $ENTITY extends AbstractAuditModel {
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            echo
            if [ "${F_KIND[$i]}" = ref ]; then
                local optional=true nullable=''
                [ "${F_NOTNULL[$i]}" = 1 ] && { optional=false; nullable=', nullable = false'; }
                cat <<EOF
    /**
     * Owning side of the relation. Excluded from {@code toString}/{@code equals} so printing
     * this row does not walk into ${F_REF[$i]} and recurse.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = $optional)
    @JoinColumn(name = "${F_COL[$i]}_id"$nullable)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ${F_JAVA[$i]} ${F_NAME[$i]};
EOF
            else
                local attrs="name = \"${F_COL[$i]}\""
                [ "${F_NOTNULL[$i]}" = 1 ] && attrs="$attrs, nullable = false"
                [ "${F_UNIQUE[$i]}" = 1 ] && attrs="$attrs, unique = true"
                [ "${F_KIND[$i]}" = text ] && attrs="$attrs, columnDefinition = \"TEXT\""
                printf '    @Column(%s)\n' "$attrs"
                printf '    private %s %s;\n' "${F_JAVA[$i]}" "${F_NAME[$i]}"
            fi
        done
        echo "}"
    } | write "erp/src/main/java/com/example/erp/$PKG/entity/$ENTITY.java"
}

# --------------------------------------------------------------------- dtos

# dto_field_lines <mode: create|update|patch|response>
dto_field_lines() {
    local mode=$1 i out=() ann java name
    for i in $(seq 0 $((NFIELDS - 1))); do
        ann=''
        if [ "${F_KIND[$i]}" = ref ]; then
            java=Long
            name="${F_NAME[$i]}Id"
        else
            java=${F_JAVA[$i]}
            name=${F_NAME[$i]}
        fi

        if [ "$mode" != response ]; then
            # A NOT NULL boolean is defaulted to false by the mapper instead of being rejected.
            if [ "$mode" != patch ] && [ "${F_NOTNULL[$i]}" = 1 ] && [ "${F_JAVA[$i]}" != Boolean ]; then
                if [ "$java" = String ]; then ann="@NotBlank"; else ann="@NotNull"; fi
            fi
            if [ "$java" = String ] && [ -n "${F_MAX[$i]}" ]; then
                ann="${ann:+$ann }@Size(max = ${F_MAX[$i]})"
            elif [ "$java" = String ] && [ "${F_KIND[$i]}" = string ]; then
                ann="${ann:+$ann }@Size(max = 255)"
            fi
            [ -n "${F_MIN[$i]}" ] && [ "$java" != String ] && ann="${ann:+$ann }@Min(${F_MIN[$i]})"
            [ -n "${F_MAX[$i]}" ] && [ "$java" != String ] && ann="${ann:+$ann }@Max(${F_MAX[$i]})"
        fi
        out+=("        ${ann:+$ann }$java $name")
    done
    if [ "$mode" = response ]; then
        out=("        Long id" "${out[@]}" "        Date createTime" "        Date lastUpdateTime")
    fi
    local n=${#out[@]}
    for i in $(seq 0 $((n - 1))); do
        if [ $i -eq $((n - 1)) ]; then printf '%s) {\n' "${out[$i]}"; else printf '%s,\n' "${out[$i]}"; fi
    done
}

dto_validation_imports() {
    local mode=$1 body
    body=$(dto_field_lines "$mode")
    case "$body" in *@NotBlank*) add_import jakarta.validation.constraints.NotBlank ;; esac
    case "$body" in *@NotNull*) add_import jakarta.validation.constraints.NotNull ;; esac
    case "$body" in *@Size*) add_import jakarta.validation.constraints.Size ;; esac
    case "$body" in *@Min*) add_import jakarta.validation.constraints.Min ;; esac
    case "$body" in *@Max*) add_import jakarta.validation.constraints.Max ;; esac
}

# gen_dto <mode> <ClassName> <javadoc>
gen_dto() {
    local mode=$1 cls=$2 doc=$3
    IMPORTS=()
    dto_validation_imports "$mode"
    add_type_imports
    [ "$mode" = response ] && add_import java.util.Date
    {
        echo "package com.example.erp.$PKG.dto;"
        echo
        emit_imports
        echo
        echo "$doc"
        echo "public record $cls("
        dto_field_lines "$mode"
        echo "}"
    } | write "erp/src/main/java/com/example/erp/$PKG/dto/$cls.java"
}

gen_dtos() {
    gen_dto create "${ENTITY}CreateRequest" "/**
 * Payload for {@code POST $ROUTE}.
 * <p>
 * The id is generated by the database, so it is deliberately absent. A NOT NULL boolean falls
 * back to false when omitted rather than being rejected.
 */"
    gen_dto update "${ENTITY}UpdateRequest" "/**
 * Payload for {@code PUT $ROUTE/{id}}: a full replacement, so an omitted optional field is
 * written as null.
 */"
    gen_dto patch "${ENTITY}PatchRequest" "/**
 * Payload for {@code PATCH $ROUTE/{id}}: every field is optional and a null one means
 * \"leave this column as it is\".
 * <p>
 * The consequence, and the reason PUT exists alongside it, is that PATCH cannot clear a column
 * back to null - null is already spoken for.
 */"
    gen_dto response "${ENTITY}Response" "/**
 * Response class for a $ENTITY. A record, so the entity itself never reaches Jackson.
 */"
}

# ------------------------------------------------------------------- mapper

# Extra mapper/service parameters for the ref fields, already resolved by the service.
ref_params() {
    local i out=''
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] && out="$out, ${F_REF[$i]} ${F_NAME[$i]}"
    done
    printf '%s' "$out"
}

ref_args() {
    local i out=''
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] && out="$out, ${F_NAME[$i]}"
    done
    printf '%s' "$out"
}

gen_mapper() {
    IMPORTS=()
    local i has_bool=0 params
    params=$(ref_params)
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_JAVA[$i]}" = Boolean ] && [ "${F_NOTNULL[$i]}" = 1 ] && has_bool=1
    done

    add_import "com.example.erp.$PKG.dto.${ENTITY}CreateRequest"
    add_import "com.example.erp.$PKG.dto.${ENTITY}PatchRequest"
    add_import "com.example.erp.$PKG.dto.${ENTITY}Response"
    add_import "com.example.erp.$PKG.dto.${ENTITY}UpdateRequest"
    add_import "com.example.erp.$PKG.entity.$ENTITY"
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] && [ "${F_REF[$i]}" != "$ENTITY" ] \
            && add_import "com.example.erp.$(printf '%s' "$(to_snake "${F_REF[$i]}")" | tr -d '_').entity.${F_REF[$i]}"
    done

    {
        echo "package com.example.erp.$PKG.mapper;"
        echo
        emit_imports
        cat <<EOF

/**
 * Entity to response, and request to entity. Kept in one place so no controller ever serializes
 * an entity and no service hand-copies fields.
 */
public final class ${ENTITY}Mapper {

    private ${ENTITY}Mapper() {
    }

    public static ${ENTITY}Response toResponse($ENTITY $CAMEL) {
        return new ${ENTITY}Response(
                $CAMEL.getId(),
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            if [ "${F_KIND[$i]}" = ref ]; then
                printf '                %s.get%s() == null ? null : %s.get%s().getId(),\n' \
                    "$CAMEL" "${F_PASCAL[$i]}" "$CAMEL" "${F_PASCAL[$i]}"
            else
                printf '                %s.get%s(),\n' "$CAMEL" "${F_PASCAL[$i]}"
            fi
        done
        cat <<EOF
                $CAMEL.getCreateTime(),
                $CAMEL.getLastUpdateTime());
    }
EOF

        # toEntity
        echo
        if [ $has_bool -eq 1 ]; then
            echo "    /** The NOT NULL flags fall back to false so an omitted field is not a constraint violation. */"
        fi
        printf '    public static %s toEntity(%sCreateRequest request%s) {\n' "$ENTITY" "$ENTITY" "$params"
        printf '        return %s.builder()\n' "$ENTITY"
        for i in $(seq 0 $((NFIELDS - 1))); do
            if [ "${F_KIND[$i]}" = ref ]; then
                printf '                .%s(%s)\n' "${F_NAME[$i]}" "${F_NAME[$i]}"
            elif [ "${F_JAVA[$i]}" = Boolean ] && [ "${F_NOTNULL[$i]}" = 1 ]; then
                printf '                .%s(orFalse(request.%s()))\n' "${F_NAME[$i]}" "${F_NAME[$i]}"
            else
                printf '                .%s(request.%s())\n' "${F_NAME[$i]}" "${F_NAME[$i]}"
            fi
        done
        echo "                .build();"
        echo "    }"

        # replace
        cat <<EOF

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace($ENTITY target, ${ENTITY}UpdateRequest request$params) {
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            if [ "${F_KIND[$i]}" = ref ]; then
                printf '        target.set%s(%s);\n' "${F_PASCAL[$i]}" "${F_NAME[$i]}"
            elif [ "${F_JAVA[$i]}" = Boolean ] && [ "${F_NOTNULL[$i]}" = 1 ]; then
                printf '        target.set%s(orFalse(request.%s()));\n' "${F_PASCAL[$i]}" "${F_NAME[$i]}"
            else
                printf '        target.set%s(request.%s());\n' "${F_PASCAL[$i]}" "${F_NAME[$i]}"
            fi
        done
        echo "    }"

        # merge
        cat <<EOF

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge($ENTITY target, ${ENTITY}PatchRequest request$params) {
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            if [ "${F_KIND[$i]}" = ref ]; then
                cat <<EOF
        if (${F_NAME[$i]} != null) {
            target.set${F_PASCAL[$i]}(${F_NAME[$i]});
        }
EOF
            else
                cat <<EOF
        if (request.${F_NAME[$i]}() != null) {
            target.set${F_PASCAL[$i]}(request.${F_NAME[$i]}());
        }
EOF
            fi
        done
        echo "    }"

        if [ $has_bool -eq 1 ]; then
            cat <<'EOF'

    private static boolean orFalse(Boolean value) {
        return value != null && value;
    }
EOF
        fi
        echo "}"
    } | write "erp/src/main/java/com/example/erp/$PKG/mapper/${ENTITY}Mapper.java"
}

# ---------------------------------------------------------------------- dao

gen_dao() {
    IMPORTS=()
    local i has_ref=0 graph=''
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] && has_ref=1
    done
    if [ $has_ref -eq 1 ]; then
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] && graph="${graph:+$graph, }\"${F_NAME[$i]}\""
        done
    fi

    add_import "com.example.erp.$PKG.entity.$ENTITY"
    add_import org.springframework.data.jpa.repository.JpaRepository
    add_import org.springframework.stereotype.Repository
    if [ $KEYWORD_IDX -ge 0 ]; then
        add_import org.springframework.data.domain.Page
        add_import org.springframework.data.domain.Pageable
    fi
    if [ $has_ref -eq 1 ]; then
        add_import org.springframework.data.jpa.repository.EntityGraph
        add_import java.util.Optional
    fi

    {
        echo "package com.example.erp.$PKG.repository;"
        echo
        emit_imports
        cat <<EOF

/**
 * $ENTITY Dao. Derived queries only; anything that needs a projection or a bulk statement is
 * spelled out with {@code @Query} rather than being pushed into a method name.
 */
@Repository
public interface ${ENTITY}Dao extends JpaRepository<$ENTITY, Long> {
EOF
        if [ $KEYWORD_IDX -ge 0 ]; then
            cat <<EOF

    Page<$ENTITY> findBy${F_PASCAL[$KEYWORD_IDX]}ContainingIgnoreCase(String ${F_NAME[$KEYWORD_IDX]}, Pageable pageable);
EOF
        fi
        if [ $has_ref -eq 1 ]; then
            cat <<EOF

    /** Pulls the lazy relation in with the row, so a detail response stays one query. */
    @Override
    @EntityGraph(attributePaths = {$graph})
    Optional<$ENTITY> findById(Long id);
EOF
        fi
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_UNIQUE[$i]}" = 1 ] || continue
            local ic=''
            [ "${F_JAVA[$i]}" = String ] && ic=IgnoreCase
            cat <<EOF

    boolean existsBy${F_PASCAL[$i]}$ic(${F_JAVA[$i]} ${F_NAME[$i]});

    /** Rejects a second row with the same ${F_NAME[$i]} on update, ignoring the row being updated. */
    boolean existsBy${F_PASCAL[$i]}${ic}AndIdNot(${F_JAVA[$i]} ${F_NAME[$i]}, Long id);
EOF
        done
        echo "}"
    } | write "erp/src/main/java/com/example/erp/$PKG/repository/${ENTITY}Dao.java"
}

# ------------------------------------------------------------------ service

gen_service() {
    IMPORTS=()
    local i sortable='"id"' params args uniques=()
    params=$(ref_params)
    args=$(ref_args)
    for i in $(seq 0 $((NFIELDS - 1))); do
        [ "${F_KIND[$i]}" = ref ] || sortable="$sortable, \"${F_NAME[$i]}\""
        [ "${F_UNIQUE[$i]}" = 1 ] && uniques+=("$i")
    done
    sortable="$sortable, \"createTime\", \"lastUpdateTime\""

    add_import com.example.common.web.dto.PageResponse
    add_import com.example.common.web.exception.ResourceNotFoundException
    add_import "com.example.erp.$PKG.dto.${ENTITY}CreateRequest"
    add_import "com.example.erp.$PKG.dto.${ENTITY}PatchRequest"
    add_import "com.example.erp.$PKG.dto.${ENTITY}Response"
    add_import "com.example.erp.$PKG.dto.${ENTITY}UpdateRequest"
    add_import "com.example.erp.$PKG.entity.$ENTITY"
    add_import "com.example.erp.$PKG.mapper.${ENTITY}Mapper"
    add_import "com.example.erp.$PKG.repository.${ENTITY}Dao"
    add_import org.springframework.data.domain.Page
    add_import org.springframework.data.domain.PageRequest
    add_import org.springframework.data.domain.Pageable
    add_import org.springframework.data.domain.Sort
    add_import org.springframework.stereotype.Service
    add_import org.springframework.transaction.annotation.Transactional
    add_import java.util.Set
    [ $KEYWORD_IDX -ge 0 ] && add_import org.springframework.util.StringUtils
    for i in $(seq 0 $((NFIELDS - 1))); do
        if [ "${F_KIND[$i]}" = ref ]; then
            local rpkg
            rpkg=$(printf '%s' "$(to_snake "${F_REF[$i]}")" | tr -d '_')
            [ "${F_REF[$i]}" != "$ENTITY" ] && add_import "com.example.erp.$rpkg.entity.${F_REF[$i]}"
            add_import "com.example.erp.$rpkg.repository.${F_REF[$i]}Dao"
        fi
    done

    {
        echo "package com.example.erp.$PKG.service;"
        echo
        emit_imports
        cat <<EOF

/**
 * CRUD for ${TABLE//_/ }.
 * <p>
 * Rules the schema cannot express live here: a uniqueness check runs before the insert so the
 * caller gets a 400 naming the field instead of a raw constraint violation, and a sort is
 * validated against an allowlist before it can reach the SQL.
 */
@Service
@Transactional(readOnly = true)
public class ${ENTITY}Service {

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS = Set.of($sortable);

    /** Paging needs a deterministic order; fall back to the id when the caller gives none. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    private final ${ENTITY}Dao ${CAMEL}Dao;
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            local rc
            rc=$(to_camel "${F_REF[$i]}")
            printf '    private final %sDao %sDao;\n' "${F_REF[$i]}" "$rc"
        done

        # constructor
        local ctor_params="${ENTITY}Dao ${CAMEL}Dao"
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            local rc
            rc=$(to_camel "${F_REF[$i]}")
            ctor_params="$ctor_params, ${F_REF[$i]}Dao ${rc}Dao"
        done
        cat <<EOF

    public ${ENTITY}Service($ctor_params) {
        this.${CAMEL}Dao = ${CAMEL}Dao;
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            local rc
            rc=$(to_camel "${F_REF[$i]}")
            printf '        this.%sDao = %sDao;\n' "$rc" "$rc"
        done
        echo "    }"

        # read
        echo
        echo "    // ------------------------------------------------------------------ read"
        if [ $KEYWORD_IDX -ge 0 ]; then
            cat <<EOF

    /** One page, optionally filtered by a case-insensitive ${F_NAME[$KEYWORD_IDX]} fragment. */
    public PageResponse<${ENTITY}Response> search(String keyword, Pageable pageable) {
        Page<$ENTITY> page = ${CAMEL}Dao.findBy${F_PASCAL[$KEYWORD_IDX]}ContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", withSafeSort(pageable));
        return PageResponse.from(page, ${ENTITY}Mapper::toResponse);
    }
EOF
        else
            cat <<EOF

    /** One page, in a deterministic order. */
    public PageResponse<${ENTITY}Response> search(Pageable pageable) {
        Page<$ENTITY> page = ${CAMEL}Dao.findAll(withSafeSort(pageable));
        return PageResponse.from(page, ${ENTITY}Mapper::toResponse);
    }
EOF
        fi
        cat <<EOF

    public ${ENTITY}Response getById(Long id) {
        return ${ENTITY}Mapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public ${ENTITY}Response create(${ENTITY}CreateRequest request) {
EOF
        for i in "${uniques[@]:-}"; do
            [ -n "$i" ] || continue
            local ic=''
            [ "${F_JAVA[$i]}" = String ] && ic=IgnoreCase
            cat <<EOF
        if (${CAMEL}Dao.existsBy${F_PASCAL[$i]}$ic(request.${F_NAME[$i]}())) {
            throw new IllegalArgumentException(
                    "${F_NAME[$i]}: a $CAMEL with ${F_NAME[$i]} '" + request.${F_NAME[$i]}() + "' already exists");
        }
EOF
        done
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            printf '        %s %s = resolve%s(request.%sId());\n' \
                "${F_REF[$i]}" "${F_NAME[$i]}" "${F_PASCAL[$i]}" "${F_NAME[$i]}"
        done
        cat <<EOF
        $ENTITY saved = ${CAMEL}Dao.save(${ENTITY}Mapper.toEntity(request$args));
        return ${ENTITY}Mapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public ${ENTITY}Response update(Long id, ${ENTITY}UpdateRequest request) {
        $ENTITY $CAMEL = findOrThrow(id);
EOF
        for i in "${uniques[@]:-}"; do
            [ -n "$i" ] || continue
            printf '        assert%sFree(request.%s(), id);\n' "${F_PASCAL[$i]}" "${F_NAME[$i]}"
        done
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            printf '        %s %s = resolve%s(request.%sId());\n' \
                "${F_REF[$i]}" "${F_NAME[$i]}" "${F_PASCAL[$i]}" "${F_NAME[$i]}"
        done
        cat <<EOF
        ${ENTITY}Mapper.replace($CAMEL, request$args);
        return ${ENTITY}Mapper.toResponse($CAMEL);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public ${ENTITY}Response patch(Long id, ${ENTITY}PatchRequest request) {
        $ENTITY $CAMEL = findOrThrow(id);
EOF
        for i in "${uniques[@]:-}"; do
            [ -n "$i" ] || continue
            cat <<EOF
        if (request.${F_NAME[$i]}() != null) {
            assert${F_PASCAL[$i]}Free(request.${F_NAME[$i]}(), id);
        }
EOF
        done
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            printf '        %s %s = resolve%s(request.%sId());\n' \
                "${F_REF[$i]}" "${F_NAME[$i]}" "${F_PASCAL[$i]}" "${F_NAME[$i]}"
        done
        cat <<EOF
        ${ENTITY}Mapper.merge($CAMEL, request$args);
        return ${ENTITY}Mapper.toResponse($CAMEL);
    }

    @Transactional
    public void delete(Long id) {
        ${CAMEL}Dao.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------- helper

    private $ENTITY findOrThrow(Long id) {
        return ${CAMEL}Dao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("$ENTITY not found with id: " + id));
    }
EOF
        for i in "${uniques[@]:-}"; do
            [ -n "$i" ] || continue
            local ic=''
            [ "${F_JAVA[$i]}" = String ] && ic=IgnoreCase
            cat <<EOF

    private void assert${F_PASCAL[$i]}Free(${F_JAVA[$i]} ${F_NAME[$i]}, Long id) {
        if (${CAMEL}Dao.existsBy${F_PASCAL[$i]}${ic}AndIdNot(${F_NAME[$i]}, id)) {
            throw new IllegalArgumentException(
                    "${F_NAME[$i]}: a $CAMEL with ${F_NAME[$i]} '" + ${F_NAME[$i]} + "' already exists");
        }
    }
EOF
        done
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            local rc
            rc=$(to_camel "${F_REF[$i]}")
            cat <<EOF

    /** Resolves the relation so a bad id answers 404 here instead of a foreign key error later. */
    private ${F_REF[$i]} resolve${F_PASCAL[$i]}(Long id) {
        if (id == null) {
            return null;
        }
        return ${rc}Dao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("${F_REF[$i]} not found with id: " + id));
    }
EOF
        done
        cat <<'EOF'

    /**
     * Rejects a sort on a property that is not in {@link #SORTABLE_FIELDS}, and supplies a
     * deterministic order when the request carries none.
     */
    private Pageable withSafeSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        sort.forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("sort: unsupported property '" + order.getProperty()
                        + "', allowed: " + SORTABLE_FIELDS);
            }
        });
        return pageable;
    }
}
EOF
    } | write "erp/src/main/java/com/example/erp/$PKG/service/${ENTITY}Service.java"
}

# --------------------------------------------------------------- controller

gen_controller() {
    IMPORTS=()
    add_import com.example.common.web.dto.ApiResponse
    add_import com.example.common.web.dto.PageResponse
    add_import "com.example.erp.$PKG.dto.${ENTITY}CreateRequest"
    add_import "com.example.erp.$PKG.dto.${ENTITY}PatchRequest"
    add_import "com.example.erp.$PKG.dto.${ENTITY}Response"
    add_import "com.example.erp.$PKG.dto.${ENTITY}UpdateRequest"
    add_import "com.example.erp.$PKG.service.${ENTITY}Service"
    add_import jakarta.validation.Valid
    add_import org.springframework.data.domain.Pageable
    add_import org.springframework.data.domain.Sort
    add_import org.springframework.data.web.PageableDefault
    add_import org.springframework.http.HttpStatus
    add_import org.springframework.http.ResponseEntity
    add_import org.springframework.web.bind.annotation.DeleteMapping
    add_import org.springframework.web.bind.annotation.GetMapping
    add_import org.springframework.web.bind.annotation.PatchMapping
    add_import org.springframework.web.bind.annotation.PathVariable
    add_import org.springframework.web.bind.annotation.PostMapping
    add_import org.springframework.web.bind.annotation.PutMapping
    add_import org.springframework.web.bind.annotation.RequestBody
    add_import org.springframework.web.bind.annotation.RequestMapping
    add_import org.springframework.web.bind.annotation.RestController
    [ $KEYWORD_IDX -ge 0 ] && add_import org.springframework.web.bind.annotation.RequestParam

    {
        echo "package com.example.erp.$PKG.controller;"
        echo
        emit_imports
        cat <<EOF

/**
 * $ENTITY CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link ${ENTITY}Response} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping("$ROUTE")
public class ${ENTITY}Controller {

    private final ${ENTITY}Service ${CAMEL}Service;

    public ${ENTITY}Controller(${ENTITY}Service ${CAMEL}Service) {
        this.${CAMEL}Service = ${CAMEL}Service;
    }
EOF
        if [ $KEYWORD_IDX -ge 0 ]; then
            cat <<EOF

    /**
     * GET $ROUTE?keyword=abc&page=0&size=10&sort=${F_NAME[$KEYWORD_IDX]},asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<${ENTITY}Response>>> get$(pluralize "$ENTITY")(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(${CAMEL}Service.search(keyword, pageable)));
    }
EOF
        else
            cat <<EOF

    /**
     * GET $ROUTE?page=0&size=10&sort=id,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<${ENTITY}Response>>> get$(pluralize "$ENTITY")(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(${CAMEL}Service.search(pageable)));
    }
EOF
        fi
        cat <<EOF

    /**
     * GET $ROUTE/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<${ENTITY}Response>> get$ENTITY(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(${CAMEL}Service.getById(id)));
    }

    /**
     * POST $ROUTE
     */
    @PostMapping
    public ResponseEntity<ApiResponse<${ENTITY}Response>> create$ENTITY(
            @Valid @RequestBody ${ENTITY}CreateRequest request) {
        ${ENTITY}Response created = ${CAMEL}Service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT $ROUTE/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<${ENTITY}Response>> replace$ENTITY(
            @PathVariable Long id, @Valid @RequestBody ${ENTITY}UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(${CAMEL}Service.update(id, request)));
    }

    /**
     * PATCH $ROUTE/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<${ENTITY}Response>> patch$ENTITY(
            @PathVariable Long id, @Valid @RequestBody ${ENTITY}PatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(${CAMEL}Service.patch(id, request)));
    }

    /**
     * DELETE $ROUTE/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete$ENTITY(@PathVariable Long id) {
        ${CAMEL}Service.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
EOF
    } | write "erp/src/main/java/com/example/erp/$PKG/controller/${ENTITY}Controller.java"
}

# ---------------------------------------------------------------- changelog

gen_changelog() {
    local next=1 f base i
    for f in "$CHANGELOG_DIR"/[0-9]*_*.yml "$CHANGELOG_DIR"/[0-9]*_*.yaml; do
        [ -e "$f" ] || continue
        base=$(basename "$f")
        base=${base%%_*}
        [ "$base" -ge "$next" ] 2>/dev/null && next=$((base + 1))
    done
    CHANGELOG_REL="erp/src/main/resources/db/changelog/${next}_${PKG}.yml"

    {
        cat <<EOF
databaseChangeLog:
  - changeSet:
      id: create-$PKG
      author: $AUTHOR
      changes:
        - createTable:
            tableName: $TABLE
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            local col=${F_COL[$i]}
            [ "${F_KIND[$i]}" = ref ] && col="${col}_id"
            echo
            cat <<EOF
              - column:
                  name: $col
                  type: ${F_SQL[$i]}
EOF
            if [ "${F_NOTNULL[$i]}" = 1 ] || [ "${F_UNIQUE[$i]}" = 1 ]; then
                echo "                  constraints:"
                [ "${F_NOTNULL[$i]}" = 1 ] && echo "                    nullable: false"
                [ "${F_UNIQUE[$i]}" = 1 ] && echo "                    unique: true"
            fi
        done
        cat <<EOF

              # Backs AbstractAuditModel from the common-jpa jar; JPA auditing fills both in.
              - column:
                  name: create_time
                  type: DATETIME
                  constraints:
                    nullable: false

              - column:
                  name: last_update_time
                  type: DATETIME
                  constraints:
                    nullable: false
EOF
        for i in $(seq 0 $((NFIELDS - 1))); do
            [ "${F_KIND[$i]}" = ref ] || continue
            local rtable
            rtable=$(pluralize "$(to_snake "${F_REF[$i]}")")
            cat <<EOF

  - changeSet:
      id: add-$PKG-${F_COL[$i]}-fk
      author: $AUTHOR
      changes:
        - addForeignKeyConstraint:
            baseTableName: $TABLE
            baseColumnNames: ${F_COL[$i]}_id
            referencedTableName: $rtable
            referencedColumnNames: id
            constraintName: fk_${PKG}_${F_COL[$i]}
EOF
        done
    } | write "$CHANGELOG_REL"

    # Register it in the master changelog. Never renumber or edit an existing entry.
    if [ $DRY_RUN -eq 1 ]; then
        echo "--- append to erp/src/main/resources/db/changelog/changelog-master.yaml"
        printf '  - include:\n      file: db/changelog/%s_%s.yml\n' "$next" "$PKG"
    elif grep -q "db/changelog/${next}_${PKG}.yml" "$MASTER"; then
        : # already registered
    else
        printf '  - include:\n      file: db/changelog/%s_%s.yml\n' "$next" "$PKG" >> "$MASTER"
        echo "erp/src/main/resources/db/changelog/changelog-master.yaml (appended include)" >> "$WRITTEN_LOG"
    fi
}

# -------------------------------------------------------------------- run it

gen_entity
gen_dtos
gen_mapper
gen_dao
gen_service
gen_controller
gen_changelog

echo
if [ $DRY_RUN -eq 1 ]; then
    echo "dry run: nothing was written"
else
    echo "Created:"
    sed 's/^/  /' "$WRITTEN_LOG"
fi
cat <<EOF

Endpoints:
  GET    $ROUTE
  GET    $ROUTE/{id}
  POST   $ROUTE
  PUT    $ROUTE/{id}
  PATCH  $ROUTE/{id}
  DELETE $ROUTE/{id}
EOF

if [ $COMPILE -eq 1 ] && [ $DRY_RUN -eq 0 ]; then
    echo
    echo "Compiling..."
    (cd "$ROOT" && ./mvnw -q -pl erp -am compile)
fi
