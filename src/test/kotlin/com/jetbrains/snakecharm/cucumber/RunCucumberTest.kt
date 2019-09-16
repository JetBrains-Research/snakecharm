package com.jetbrains.snakecharm.cucumber

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
        plugin = ["pretty"],
        tags=["~@ignore"],
        glue = ["features.glue"]
        // plugin = ["pretty", "json:target/cucumber-report.json"]
        //plugin = ["json:target/cucumber-report.json"]
)
class AllCucumberFeaturesTest