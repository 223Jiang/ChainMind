package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemporaryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
@Mapper
public interface TemporaryMapper extends BaseMapper<TemporaryDO> {
}
