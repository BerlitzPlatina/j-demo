# Gacha Engine

Gacha Engine là module cho phép thực hiện các chức năng random và chọn phần thưởng trong hệ thống gacha. Nó cung cấp một hệ thống pity hoàn chỉnh, quản lý hồ sơ người dùng, lịch sử rút, và kho hàng.

## 🎯 Tính năng

### ✅ Hệ thống Pity
- **Hard Pity (90 pulls)**: Đảm bảo rút được 5-star
- **Soft Pity (75+ pulls)**: Tăng tỷ lệ rút được featured item
- **Guaranteed System**: Xoay phiên giữa featured và non-featured 5-star
- **4-star Pity (10 pulls)**: Đảm bảo rút được 4-star

### 📊 Quản lý Hồ sơ Người dùng
- Theo dõi pity counters cho mỗi banner
- Lưu trữ lịch sử guaranteed pulls
- Lưu trữ item cuối cùng 5-star nhận được

### 📜 Lịch sử Rút
- Ghi lại toàn bộ thông tin mỗi lần rút (pity status, featured, rarity, v.v.)
- Hỗ trợ truy vấn lịch sử theo user, banner, rarity
- Business key cho mỗi pull (UUID-based code)

### 🎒 Kho Hàng Người dùng
- Theo dõi số lượng items sở hữu
- Ghi lại số lần lấy được item
- Quản lý trạng thái active/inactive

### 📈 Thống Kê
- Tổng số pulls
- Đếm 5-star, 4-star, 3-star pulls
- Tỷ lệ phần trăm mỗi rarity
- Pulls kể từ lần cuối nhận 5-star/4-star

## 🏗️ Kiến trúc

```
gachaengine/
├── entity/
│   ├── UserProfile.java        (Hồ sơ người dùng)
│   ├── PullHistory.java        (Lịch sử rút)
│   ├── UserInventory.java      (Kho hàng)
│   └── PullType.java           (Enum loại rút)
├── repository/
│   ├── UserProfileDao.java     (JPA repository)
│   ├── PullHistoryDao.java     (JPA repository)
│   └── UserInventoryDao.java   (JPA repository)
├── service/
│   ├── GachaEngine.java                (Core engine - random & pity logic)
│   ├── UserProfileService.java         (Quản lý hồ sơ)
│   ├── PullHistoryService.java         (Quản lý lịch sử)
│   └── UserInventoryService.java       (Quản lý kho hàng)
├── dto/
│   ├── PullRequest.java                (Request DTO)
│   ├── PullResultResponse.java         (Single pull result)
│   ├── MultiPullResultResponse.java    (Multi pull result)
│   ├── UserProfileResponse.java        (Profile DTO)
│   ├── PullStatisticsResponse.java     (Statistics DTO)
│   └── InventoryItemResponse.java      (Inventory item DTO)
└── controller/
    ├── GachaEngineController.java      (Pull API)
    ├── UserProfileController.java      (Profile API)
    ├── PullHistoryController.java      (History API)
    └── UserInventoryController.java    (Inventory API)
```

## 📡 API Endpoints

### Gacha Pulls
```
POST /api/gacha/pull
  Request: {
    "userCode": "user123",
    "bannerCode": "banner_permanent",
    "quantity": 1
  }
  Response: MultiPullResultResponse with results, statistics, and profile
```

### User Profiles
```
GET /api/gacha/profile/{userCode}/{bannerCode}
  Response: UserProfileResponse
  
GET /api/gacha/profiles/{userCode}
  Response: List<UserProfileResponse> (all banners)
  
PUT /api/gacha/profile/{userCode}/{bannerCode}/guarantee?guaranteed=true
  Response: UserProfileResponse
  
PUT /api/gacha/profile/{userCode}/{bannerCode}/reset
  Response: UserProfileResponse (reset pity)
```

### Pull History
```
GET /api/gacha/history/{userCode}/{bannerId}
  Response: List<PullResultResponse> (newest first)
  
GET /api/gacha/history/{userCode}/{bannerId}/latest?limit=10
  Response: List<PullResultResponse> (latest N pulls)
  
GET /api/gacha/history/{userCode}/all
  Response: List<PullResultResponse> (all banners)
  
GET /api/gacha/history/{userCode}/rarity?minRarity=5
  Response: List<PullResultResponse> (by rarity)
```

### Inventory
```
GET /api/gacha/inventory/{userCode}
  Response: List<InventoryItemResponse>
  
GET /api/gacha/inventory/{userCode}/active
  Response: List<InventoryItemResponse> (active only)
  
GET /api/gacha/inventory/{userCode}/item/{itemId}
  Response: InventoryItemResponse
```

### Statistics
```
GET /api/gacha/statistics/{userCode}/{bannerId}
  Response: PullStatisticsResponse
```

## 🔑 Business Keys

Thiết kế microservice - mỗi resource có cả `id` và `code`:

### User Profile
- **id**: Khóa chính internal (Long)
- **userCode**: Business key từ external service (String)
- **bannerId**: Internal reference
- **bannerCode**: Business key từ external service

### Pull History
- **id**: Khóa chính internal (Long)
- **code**: Business key - UUID (String)
- **userCode**: Business key người dùng
- **itemCode**: Business key item

### User Inventory
- **id**: Khóa chính internal (Long)
- **userCode**: Business key người dùng
- **itemCode**: Business key item

## 🎲 Random Logic

### Phân loại Rarity

1. **Kiểm tra Hard Pity**: Nếu pity_counter >= 90 → rút 5-star
2. **Soft Pity**: Nếu soft_pity_counter >= 75 → tăng tỷ lệ 5-star (x3)
3. **Normal**: Sử dụng base rates từ BannerRarityRate
4. **4-star Guaranteed**: Nếu guaranteed_counter >= 10 → rút 4-star

### Featured vs Non-featured

- **5-star**: Sử dụng guaranteed system (xoay phiên)
- **4-star**: Dùng featured rate từ BannerRarityRate
- **3-star**: Mặc định non-featured

### Chọn Item Cụ Thể

- Nhóm items theo (rarity, featured)
- Tính tổng weight trong nhóm
- Random dựa trên weight distribution

## 💾 Database Schema

### user_profiles
- id (PK)
- user_code (FK external)
- banner_id (FK banners)
- banner_code
- total_pulls
- pity_counter
- guaranteed_counter
- is_guaranteed
- last_guaranteed_item_id
- last_five_star_item_id
- soft_pity_counter
- notes
- active
- created_at, updated_at
- **Unique**: (user_code, banner_id)

### pull_histories
- id (PK)
- code (Business key, unique)
- user_code
- user_profile_id (FK)
- banner_id (FK)
- banner_code
- item_id (FK)
- item_code
- rarity
- is_featured
- pull_type (SINGLE, MULTI_10, MULTI_CUSTOM)
- pity_at_pull
- guaranteed_at_pull
- soft_pity_at_pull
- was_guaranteed
- notes
- created_at, updated_at
- **Indexes**: (user_code, banner_id), user_code, item_id

### user_inventories
- id (PK)
- user_code
- item_id (FK)
- item_code
- quantity
- times_obtained
- notes
- active
- created_at, updated_at
- **Unique**: (user_code, item_id)

## 🔄 Quy trình Pull

```
1. Nhận request: userCode, bannerCode, quantity
2. Tìm banner theo code (hoặc tạo nếu không tìm thấy profile)
3. Lấy user profile từ DB
4. Lặp lại quantity lần:
   a. Xác định rarity dựa trên pity system
   b. Lấy pool items cho rarity đó
   c. Quyết định featured vs non-featured
   d. Random chọn item từ pool
   e. Update profile pity counters
   f. Lưu pull history
   g. Update inventory
5. Tính toán statistics
6. Trả về MultiPullResultResponse
```

## 🧪 Ví dụ Sử dụng

### Single Pull
```bash
curl -X POST http://localhost:8080/api/gacha/pull \
  -H "Content-Type: application/json" \
  -d '{
    "userCode": "player001",
    "bannerCode": "limited_banner_v1",
    "quantity": 1
  }'
```

### 10-Pull
```bash
curl -X POST http://localhost:8080/api/gacha/pull \
  -H "Content-Type: application/json" \
  -d '{
    "userCode": "player001",
    "bannerCode": "limited_banner_v1",
    "quantity": 10
  }'
```

### Lấy Lịch sử Rút
```bash
curl http://localhost:8080/api/gacha/history/player001/1/latest?limit=20
```

### Lấy Thống kê
```bash
curl http://localhost:8080/api/gacha/statistics/player001/1
```

## 📝 Ghi chú

- Tất cả pulls được ghi lại với snapshot của pity status
- Hệ thống đảm bảo tính atomic - mỗi pull là 1 transaction
- Có thể reset pity cho maintenance/special events
- Guaranteed status có thể được set manually (admin)
- Soft pity counter riêng biệt để tracking rate-up zone

## 🚀 Future Enhancements

- [ ] Batch pull simulations (dry run)
- [ ] Pull history export/archive
- [ ] Pity system analytics
- [ ] Event-based rates rotation
- [ ] Custom weight calculations
- [ ] A/B testing framework
