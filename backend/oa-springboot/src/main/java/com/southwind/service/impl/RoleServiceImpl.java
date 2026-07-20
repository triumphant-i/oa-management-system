package com.southwind.service.impl;

import com.southwind.entity.Role;
import com.southwind.mapper.RoleMapper;
import com.southwind.service.RoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}