package com.larkmt.cn.admin.controller;

import com.larkmt.cn.admin.core.util.I18nUtil;
import com.larkmt.cn.admin.entity.JobUser;
import com.larkmt.cn.admin.mapper.JobUserMapper;
import com.larkmt.cn.admin.service.JobUserService;
import com.larkmt.core.biz.model.ReturnT;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.larkmt.core.biz.model.ReturnT.FAIL_CODE;

/**
 * @Author: LarkMidTable
 * @Date: 2020/9/16 11:14
 * @Description: 用户管理模块
 **/
@RestController
@RequestMapping("/larkmidtable/api/user")
@Api(tags = "用户信息接口")
@Slf4j
public class UserController {

    @Resource
    private JobUserMapper jobUserMapper;

    @Resource
    private JobUserService jobUserService;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @GetMapping("/pageList")
    @ApiOperation("用户列表")
    public ReturnT<Map<String, Object>> pageList(@RequestParam(value = "current", required = false, defaultValue = "1") int current,
                                                 @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                                                 @RequestParam(value = "username", required = false) String username) {
        // page list
        List<JobUser> list = jobUserMapper.pageList((current - 1) * size, size, username);
        int recordsTotal = jobUserMapper.pageListCount((current - 1) * size, size, username);

        // package result
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", recordsTotal);        // 总记录数
        maps.put("recordsFiltered", recordsTotal);    // 过滤后的总记录数
        maps.put("data", list);                    // 分页列表
        return new ReturnT<>(maps);
    }

    @GetMapping("/list")
    @ApiOperation("用户列表")
    public ReturnT<List<JobUser>> list(String username) {

        // page list
        List<JobUser> list = jobUserMapper.findAll(username);
        return new ReturnT<>(list);
    }

    @GetMapping("/getUserById")
    @ApiOperation(value = "根据id获取用户")
    public ReturnT<JobUser> selectById(@RequestParam("userId") Integer userId) {
        return new ReturnT<>(jobUserMapper.getUserById(userId));
    }

    @PostMapping("/add")
    @ApiOperation("添加用户")
    public ReturnT<String> add(@RequestBody JobUser jobUser) throws Exception {
        return jobUserService.addJobUser(jobUser);
    }

    @PostMapping(value = "/update")
    @ApiOperation("更新用户信息")
    public ReturnT<String> update(@RequestBody JobUser jobUser) throws Exception {
        return jobUserService.updateJobUser(jobUser);
    }

    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    @ApiOperation("删除用户")
    public ReturnT<String> remove(int id) {
        int result = jobUserService.removeJobUser(id);
        return result != 1 ? ReturnT.FAIL : ReturnT.SUCCESS;
    }

    @PostMapping(value = "/updatePwd")
    @ApiOperation("修改密码")
    public ReturnT<String> updatePwd(@RequestBody JobUser jobUser) {
        String password = jobUser.getPassword();
        if (password == null || password.trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        password = password.trim();
        if (!(password.length() >= 4 && password.length() <= 20)) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
        }
        // do write
        JobUser existUser = jobUserMapper.loadByUserName(jobUser.getUsername());
        existUser.setPassword(bCryptPasswordEncoder.encode(password));
        jobUserMapper.update(existUser);
        return ReturnT.SUCCESS;
    }

    @RequestMapping(value = "/deleteTheUser/{correlationId}", method = RequestMethod.POST)
    @ApiOperation("删除用户v2.0")
    public ReturnT<String> deleteTheUser(@PathVariable String correlationId) {
        int result = jobUserMapper.deleteTheUser(correlationId);
        return result != 1 ? ReturnT.FAIL : ReturnT.SUCCESS;
    }
}
