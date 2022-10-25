package com.nextlabs.rms.cli;

import com.nextlabs.rms.Config;

public interface Action {

    void execute(Config config, String[] args);
}
