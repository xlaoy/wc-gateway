package com.xlaoy.wcgateway.system;

import com.xlaoy.wcgateway.support.ResourceKeysHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Administrator on 2018/8/3 0003.
 */
@RestController
public class SystemController {

    @Autowired
    private ResourceKeysHolder resourceKeysHolder;

    @GetMapping("/refesh_resource_keys")
    public void refeshResourceKeys() {
        resourceKeysHolder.refeshResourceKeys();
    }

}
