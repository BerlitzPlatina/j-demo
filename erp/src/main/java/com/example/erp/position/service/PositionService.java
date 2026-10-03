package com.example.erp.position.service;

import com.example.erp.position.repository.PositionDao;
import com.example.erp.position.entity.Position;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PositionService {
    private final PositionDao positionDao;

    public PositionService(PositionDao positionDao) {
        this.positionDao = positionDao;
    }

    public List<Position> getAllPositions() {
        List<Position> positions = positionDao.findAll();

        for (Position position : positions) {
            System.out.println(
                    position.getLegalEntity().getEntityCode());
        }
        return positions;
    }
}
