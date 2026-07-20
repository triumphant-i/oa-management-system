package com.southwind.service.impl;

import com.southwind.entity.Dict;
import com.southwind.mapper.DictMapper;
import com.southwind.service.DictService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {
}