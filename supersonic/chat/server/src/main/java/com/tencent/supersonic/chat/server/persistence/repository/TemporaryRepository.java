package com.tencent.supersonic.chat.server.persistence.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tencent.supersonic.chat.api.pojo.request.TemporaryReq;
import com.tencent.supersonic.chat.api.pojo.request.TemporaryUpdateReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemporaryDO;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
public interface TemporaryRepository extends IService<TemporaryDO> {
    /**
     * 搜索临时表
     * @param temporaryReq  请求数据
     * @return              临时表集合
     */
    Page<TemporaryDO> search(TemporaryReq temporaryReq);

    /**
     * 更新临时表
     * @param temporaryUpdateReq    修改请求数据
     */
    void update(TemporaryUpdateReq temporaryUpdateReq);
}
