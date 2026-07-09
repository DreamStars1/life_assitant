package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;

import java.util.List;

@Mapper
public interface MediaCommentMapper extends BaseMapper<MediaCommentDO> {

    @Select("SELECT * FROM media_comment WHERE media_id = #{mediaId} ORDER BY created_at ASC")
    List<MediaCommentDO> selectByMediaId(@Param("mediaId") String mediaId);
}
