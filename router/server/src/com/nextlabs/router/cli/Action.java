package com.nextlabs.router.cli;

import com.nextlabs.router.Config;

public interface Action {

    void execute(Config config, String[] args);
}
