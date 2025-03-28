package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.chat.api.pojo.request.TemporaryReq;
import com.tencent.supersonic.chat.api.pojo.request.TemporaryUpdateReq;
import com.tencent.supersonic.chat.server.persistence.repository.TemporaryRepository;
import com.tencent.supersonic.chat.server.persistence.mapper.TemporaryMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemporaryDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
@Slf4j
@Repository
public class TemporaryRepositoryImpl extends ServiceImpl<TemporaryMapper, TemporaryDO>
        implements TemporaryRepository {
    @Override
    public Page<TemporaryDO> search(TemporaryReq temporaryReq) {
        LambdaQueryWrapper<TemporaryDO> wrapper = new LambdaQueryWrapper<>();
        // 根据用户id、表名、表说明等条件查询（默认查询未删除数据，同时时间倒叙排序）
        wrapper.eq(TemporaryDO::getUserId, temporaryReq.getUserId())
               .like(StringUtils.isNotBlank(temporaryReq.getTableType()),
                        TemporaryDO::getTableType, temporaryReq.getTableType())
               .like(StringUtils.isNotBlank(temporaryReq.getTableNote()),
                        TemporaryDO::getTableNote, temporaryReq.getTableNote())
                .eq(TemporaryDO::getIsDelete, 0)
                .orderByDesc(TemporaryDO::getCreateTime);

        long total = this.count(wrapper);
        return this.page(Page.of(temporaryReq.getCurrent(), temporaryReq.getPageSize(), total), wrapper);
    }

    @Override
    public void update(TemporaryUpdateReq temporaryUpdateReq) {
        TemporaryDO temporaryDO = TemporaryDO.builder()
                .id(temporaryUpdateReq.getId())
                .tableNote(temporaryUpdateReq.getTableNote())
                .tableType(temporaryUpdateReq.getTableType()).build();

        updateById(temporaryDO);
    }
}
