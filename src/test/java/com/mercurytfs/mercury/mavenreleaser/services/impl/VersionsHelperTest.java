package com.mercurytfs.mercury.mavenreleaser.services.impl;

import com.mercurytfs.mercury.mavenreleaser.helpers.NewVersionHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class VersionsHelperTest {


    @Test
    public void updateJirasTestProjectNotFound(){
        assertEquals("1.1.0-SNAPSHOT",NewVersionHelper.getNextVersion("1.0.0",""));
        assertEquals("1.0.2-SNAPSHOT",NewVersionHelper.getNextVersion("1.0.1",""));
        assertEquals("1.1.0-SNAPSHOT",NewVersionHelper.getNextVersion("1.0.0-SNAPSHOT",""));
        assertEquals("1.0.2-SNAPSHOT",NewVersionHelper.getNextVersion("1.0.1-SNAPSHOT",""));
    }

}
