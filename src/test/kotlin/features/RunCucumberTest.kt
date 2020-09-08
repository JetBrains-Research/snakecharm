package features

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
        plugin = ["pretty", "summary"],
        tags=["~@ignore"]
//        glue = ["features.glue"]
        // plugin = ["pretty", "json:target/cucumber-report.json"]
        //plugin = ["json:target/cucumber-report.json"]
)
class AllCucumberFeaturesTest