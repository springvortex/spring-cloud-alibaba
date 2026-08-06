package com.zjc.provider.controller;

import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.User;
import com.zjc.provider.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Resource
    private UserService userService;


    @GetMapping("/user/{id}")
    public ApiResponse<User> getUser(@PathVariable("id") Long id) {
        User user = userService.selectUserById(id);
        return ApiResponse.success(user);
    }

}
