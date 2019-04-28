package hellocucumber

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
////        features = ["/Users/romeo/work/git_else/snakecharm/src/test/kotlin/hellocucumber"],
        plugin = ["pretty"]
)
class RunCucumberTest