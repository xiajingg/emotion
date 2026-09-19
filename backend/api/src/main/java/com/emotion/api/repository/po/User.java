package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiajing
 * @since 2024-03-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user")
public class User implements Serializable, UserDetails {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("nickname")
    private String nickname;

    @TableField("constellation")
    private String constellation;

    @TableField("password")
    private String password;
    // 加盐
    @TableField("salt")
    private String salt;


//    权限列表
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

//    是否过期, 否
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
// 是否锁定, 否
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
// 密码是否过期
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
// 账户是否可用
    @Override
    public boolean isEnabled() {
        return true;
    }
}
