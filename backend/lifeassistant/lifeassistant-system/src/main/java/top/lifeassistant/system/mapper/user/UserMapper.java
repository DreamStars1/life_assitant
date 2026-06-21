package top.lifeassistant.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.system.model.entity.user.UserDO;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
    @Select("SELECT * FROM user WHERE email = #{email}")
    UserDO selectByEmail(@Param("email") String email);
}
