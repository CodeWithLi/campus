package campus.ai.rag.service.service.security;

import campus.ai.rag.service.entity.rbac.RbacUserEntity;
import campus.ai.rag.service.mapper.rbac.RbacUserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final RbacUserMapper rbacUserMapper;

    public AdminUserDetailsService(RbacUserMapper rbacUserMapper) {
        this.rbacUserMapper = rbacUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        RbacUserEntity userEntity = rbacUserMapper.findByUsername(username);
        if (userEntity == null || userEntity.getStatus() == null || userEntity.getStatus() != 1) {
            throw new UsernameNotFoundException("管理员账号不存在或已禁用");
        }

        List<String> roleCodes = rbacUserMapper.findRoleCodesByUsername(username);
        List<GrantedAuthority> authorities = roleCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return User.withUsername(userEntity.getUsername())
                .password(userEntity.getPassword())
                .authorities(authorities)
                .build();
    }
}

