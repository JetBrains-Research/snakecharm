[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
License [![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/MIT)
Tests Linux [![tests](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:SnakeCharmPlugin_TestsLinux)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=SnakeCharmPlugin_TestsLinux&guest=1)
Tests Windows [![tests](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:SnakeCharmPlugin_TestsWindows)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=SnakeCharmPlugin_TestsWindows&guest=1)

# About
<!-- Plugin description -->
**SnakeCharm** plugin for [PyCharm](https://www.jetbrains.com/pycharm/) / [IntelliJ Platform IDEs](https://www.jetbrains.com/products.html?fromMenu#type=ide) adds IDE support for [Snakemake](https://snakemake.readthedocs.io/en/stable/) workflows language, that is widely used in Bioinformatics. The plugin is developed by JetBrains Research Team, for more details see [project home page](https://research.jetbrains.org/groups/biolabs/projects?project_id=57).

Please report features suggestions or found bugs to project [issue tracker](https://github.com/JetBrains-Research/snakecharm/issues).

**Features highlights:**
> **NB**: To activate all features please enable `Snakemake` support in `Settings | Languages & Frameworks | Snakemake`. Snakemake framework should be configured to use python interpreter with `snakemake` module installed.

Features available in `Snakefile` and `*.smk`, `*.rule`, `*.rules` files:
* Python code syntax highlighting
* Snakemake specific syntax highlighting
    * Highlight rule section names 
    * Syntax highlighting for Snakemake string format language, e.g. in strings like `"results/sample_{genome}.bam"`.
    * Special highlighting for rule wildcards and their usages
* Code completion and goto to declaration for:
  * Snakemake specific variables (e.g. `config`, `rules`,  etc);
  * Snakemake api methods like `expand`, `temp`, etc.
  * Rules names after `rules.`, checkpoints after `checkpoints.`
  * Rules sections names after `rules.my_rule_name.`
  * Rule sections keywords arguments, e.g. after `rules.my_rule_name.input.`
  * Wildcards and sections names in `shell`, `input`, `message`, `params` and other sections
  * Paths in sections: `configfile`, `workdir`, `conda`, `include`.
  * Completion for wrappers names in `wrapper:` sections
  * Wrappers with detailed `meta.yaml` provides completion for rule section keyword arguments in `output`, `input`, `params` sections
  * Python methods and variables

* Structure view for Snakemake files
* Rules blocks folding
* Open rule declaration by name using `Navigate|Symbol...`
* Quick documentation for wrappers, that includes links to wrapper source code and paga in wrappers repository
* Multiple code inspections for possible errors in snakemake files
  
**Useful links:**
* [Project Home Page](https://research.jetbrains.org/groups/biolabs/projects?project_id=57)
* [Full Changelog](https://github.com/JetBrains-Research/snakecharm/blob/master/CHANGELOG.md)
* [SnakeCharm at GitHub](https://github.com/JetBrains-Research/snakecharm)           
* [SnakeCharm in JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/11947-snakecharm)
<!-- Plugin description end -->

# Installation
    
**Via JetBrains Plugins Market Place (recommended):**
> **NB**: If you've already configured custom file type for `Snakefile` or `*.smk` files please **delete** it (**or change** file masks to smth else). Otherwise, your custom file type will replace SnakeCharm support in snakemake files.

In PyCharm (or other `IntelliJ Platfrom` based IDE with `Python Community Edition` plugin installed) open `Preferences|Plugins|Marketplace|SnakeCharm` and press `Install` button.
     
**From ZIP plugin bundle (alternative way):**

* Open IDEA/PyCharm Preferences
* Choose `Plugins` section
* Press gear icon and choose `Install Plugin from Disk...`
* Use `*.zip` bundle download from Plugin Manager or built from sources 


# Setup Snakemake support

To enable all code insight features (e.g. code completion for `directory`, `touch` etc methods) please configure Python SDK with installed snakemake package.

https://github.com/JetBrains-Research/snakecharm/wiki#setup-snakemake-support

# IntelliJ Plugins Repository
Plugin is available in IntelliJ Plugins Repository, see [SnakeCharm Plugin](https://plugins.jetbrains.com/plugin/11947-snakecharm).

# EAP Updates
Early builds of coming releases are available in EAP channel. To receive this updates add EAP repository:
* Open IntelliJ IDEA / PyCharm / .. IDE setting
* In Plugins section press 'gear' icon, select `Manage Plugin Repositories...`
* Add repo `https://plugins.jetbrains.com/plugins/eap/list` using `+` button, press OK
(Complete guide at [Custom Release Channels](https://www.jetbrains.org/intellij/sdk/docs/plugin_repository/custom_channels.html))
* EAP builds will be visible as plugin updates

# Support
Contact me at roman.chernyatchik@jetbrains.com or post issues in [Issue Tracker](https://github.com/JetBrains-Research/snakecharm/issues)

---

# Development

Pull requests are welcome. It is my side project, so I appreciate your help with implementation of desired features.
See [DEVELPER.md](DEVELOPER.md)