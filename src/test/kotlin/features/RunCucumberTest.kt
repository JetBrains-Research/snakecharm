package features

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    plugin = ["pretty", "summary"],
    tags = "not @ignore"
//        tags="not @ignore and @here"
//        tags="@ignore"
)
class AllCucumberFeaturesTest