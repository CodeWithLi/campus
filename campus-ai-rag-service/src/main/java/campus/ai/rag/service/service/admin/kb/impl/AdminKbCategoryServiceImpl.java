package campus.ai.rag.service.service.admin.kb.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.kb.KbCategoryCreateDTO;
import campus.ai.rag.service.dto.admin.kb.KbCategoryUpdateDTO;
import campus.ai.rag.service.entity.kb.KbCategoryEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.kb.KbCategoryMapper;
import campus.ai.rag.service.service.admin.kb.AdminKbCategoryService;
import campus.ai.rag.service.vo.admin.kb.KbCategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminKbCategoryServiceImpl implements AdminKbCategoryService {

    private final KbCategoryMapper kbCategoryMapper;

    public AdminKbCategoryServiceImpl(KbCategoryMapper kbCategoryMapper) {
        this.kbCategoryMapper = kbCategoryMapper;
    }

    @Override
    public KbCategoryVO create(KbCategoryCreateDTO dto) {
        validateCreateDto(dto);

        KbCategoryEntity exists = kbCategoryMapper.selectOne(new LambdaQueryWrapper<KbCategoryEntity>()
                .eq(KbCategoryEntity::getCategoryCode, dto.getCategoryCode().trim())
                .last("limit 1"));
        if (Objects.nonNull(exists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "分类编码已存在");
        }

        KbCategoryEntity entity = new KbCategoryEntity();
        entity.setCategoryCode(dto.getCategoryCode().trim());
        entity.setCategoryName(dto.getCategoryName().trim());
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        entity.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        entity.setRemark(trimOrNull(dto.getRemark()));
        kbCategoryMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public KbCategoryVO update(Long id, KbCategoryUpdateDTO dto) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "分类ID不能为空");
        }
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "更新参数不能为空");
        }

        KbCategoryEntity entity = kbCategoryMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "分类不存在");
        }

        if (!isBlank(dto.getCategoryName())) {
            entity.setCategoryName(dto.getCategoryName().trim());
        }
        if (Objects.nonNull(dto.getEnabled())) {
            entity.setEnabled(dto.getEnabled());
        }
        if (Objects.nonNull(dto.getSortNo())) {
            entity.setSortNo(dto.getSortNo());
        }
        if (Objects.nonNull(dto.getRemark())) {
            entity.setRemark(trimOrNull(dto.getRemark()));
        }
        kbCategoryMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public void delete(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "分类ID不能为空");
        }
        KbCategoryEntity entity = kbCategoryMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "分类不存在");
        }
        kbCategoryMapper.deleteById(id);
    }

    @Override
    public KbCategoryVO getById(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "分类ID不能为空");
        }
        KbCategoryEntity entity = kbCategoryMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "分类不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<KbCategoryVO> listAll() {
        List<KbCategoryEntity> list = kbCategoryMapper.selectList(new LambdaQueryWrapper<KbCategoryEntity>()
                .orderByDesc(KbCategoryEntity::getEnabled)
                .orderByAsc(KbCategoryEntity::getSortNo)
                .orderByDesc(KbCategoryEntity::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    private void validateCreateDto(KbCategoryCreateDTO dto) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "创建参数不能为空");
        }
        if (isBlank(dto.getCategoryCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "分类编码不能为空");
        }
        if (isBlank(dto.getCategoryName())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "分类名称不能为空");
        }
    }

    private KbCategoryVO toVO(KbCategoryEntity entity) {
        KbCategoryVO vo = new KbCategoryVO();
        vo.setId(entity.getId());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setEnabled(entity.getEnabled());
        vo.setSortNo(entity.getSortNo());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trim = value.trim();
        return trim.isEmpty() ? null : trim;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

