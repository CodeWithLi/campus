package campus.ai.rag.service.mapper.rbac;

import campus.ai.rag.service.entity.rbac.RbacUserRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RbacUserRoleMapper extends BaseMapper<RbacUserRoleEntity> {
}

