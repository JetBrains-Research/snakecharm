[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
License [![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/MIT)
Tests Linux [![tests](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:SnakeCharmPlugin_TestsLinux)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=SnakeCharmPlugin_TestsLinux&guest=1)
Tests Windows [![tests](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:SnakeCharmPlugin_TestsWindows)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=SnakeCharmPlugin_TestsWindows&guest=1)

# About

**SnakeCharm** is a plugin for [PyCharm](https://www.jetbrains.com/pycharm/) / [IntelliJ Platform IDEs](https://www.jetbrains.com/products.html?fromMenu#type=ide) which adds support for [Snakemake](https://snakemake.readthedocs.io/en/stable/) workflows language. The plugin is developed by JetBrains Research Team, for more details see [project home page](https://research.jetbrains.org/groups/biolabs/projects?project_id=57).


Please report features suggestions or found bugs to project [issue tracker](https://github.com/JetBrains-Research/snakecharm/issues).

# Features

Features available in `Snakefile` and `*.smk`, `*.rule`, `*.rules` files:
* Python code syntax highlighting
* Snakemake specific syntax highlighting
* Limited resolve and code completion for python code
* Rules folding
* Code completion and resolve for:
   * Rules names after `rules.`, checkpoints after `checkpoints.`
   * Rules sections names after `rules.my_rule_name.`
   * Rule sections keywords arguments, e.g. after ``rules.my_rule_name.input.`
   * Snakemake api methods like `expand`, `temp`, etc.
   * Wildcards and sections names in `shell` and other sections
   * Included rules files and conda environment files
* File structure view with rules
* Wildcards support (code completion, resolve, usages highligting, inspections)
* Multiple inspections inspections for possible errors in snakemake file.

# Installation

If you've already configured custom file type for `Snakefile` or `*.smk` files please **delete** it (**or change** file masks to smth else). Otherwise, your custom file type will replace SnakeCharm support in snakemake files.

In PyCharm (or other `IntelliJ Platfrom` based IDE with `Python Community Edition` plugin installed) open `Preferences|Plugins|Marketplace|SnakeCharm` and press `Install` button.

At the moment the plugin is tested with:
* IDEA CI 2018.3-2019.2.x, Python Community Edition Plugin: 2018.3.183.4284.36-2019.1.191.7479.19
* PyCharm 2018.3.1-2020.2.EAP (Professional & Community Editions)

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

Pull requests are welcome. It is my side project, so I appreciate your help implementing plugin desired features.

**Build plugin from sources:**
* Run `./gradlew buildPlugin`
* Plugin bundle is located in ` build/distributions/snakecharm-*.zip`

**Install plugin from ZIP bundle:**
* Open IDEA/PyCharm Preferences
* Choose `Plugins` section
* Press gear icon and choose `Install Plugin from Disk...`
* Use `*.zip` bundle download from Plugin Manager or built from sources 

**Tests:**
Tests are written in [Gherkin](https://cucumber.io/docs/gherkin):

If you get "Unimplemented substep definition" in all `*.feature` files, ensure:
  * Not installed or disabled: `Substeps IntelliJ Plugin` 
  * Plugins installed: `Cucumber Java`, `Gherkin`
  
**Release plugin:**
* Fix version in `build.gradle`
* Fix since/until build versions in `build.gradle`
* Fix change notes in `CHANGES` file
* Use 'publishPlugin' task
