package com.tencent.supersonic.headless.server.pojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认的数据库参数构建服务实现类 该类实现了DbParametersBuilder接口，用于构建一组默认的数据库参数
 * 
 * @author JiangWeiWei
 */
@Service
@Slf4j
public class DefaultParametersBuilder implements DbParametersBuilder {

    /**
     * 构建并返回一组默认的数据库参数 这些参数包括数据库的链接URL、用户名和密码，每个参数都有相应的注释和占位符提示
     *
     * @return 包含数据库参数的列表
     */
    @Override
    public List<DatabaseParameter> build() {
        // 初始化数据库参数列表
        List<DatabaseParameter> databaseParameters = new ArrayList<>();

        // 创建并配置数据库链接参数
        DatabaseParameter host = new DatabaseParameter();
        host.setComment("链接");
        host.setName("url");
        host.setPlaceholder("请输入链接");
        // 将链接参数添加到参数列表中
        databaseParameters.add(host);

        // 创建并配置数据库用户名参数
        DatabaseParameter userName = new DatabaseParameter();
        userName.setComment("用户名");
        userName.setName("username");
        userName.setPlaceholder("请输入用户名");
        // 将用户名参数添加到参数列表中
        databaseParameters.add(userName);

        // 创建并配置数据库密码参数
        DatabaseParameter password = new DatabaseParameter();
        password.setComment("密码");
        password.setName("password");
        password.setPlaceholder("请输入密码");
        // 将密码参数添加到参数列表中
        databaseParameters.add(password);

        // 返回构建好的数据库参数列表
        return databaseParameters;
    }
}
