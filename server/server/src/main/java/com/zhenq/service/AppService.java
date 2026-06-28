package com.zhenq.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zhenq.model.dto.app.AppQueryRequest;
import com.zhenq.model.entity.App;
import com.zhenq.model.vo.AppVO;

import java.util.List;

/**
 * 应用服务
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用视图（含创建者信息）
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图列表（批量填充创建者信息）
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询请求构造查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
