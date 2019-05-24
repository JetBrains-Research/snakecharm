[![JetBrains team project](https://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
License [![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/MIT)
Tests Linux [![tests](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:SnakeCharmPlugin_TestsLinux)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=Epigenome_Tests&guest=1)
Tests Windows [![tests](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:SnakeCharmPlugin_TestsWindows)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=Epigenome_TestsWindows&guest=1)

# About

**SnakeCharm** is a plugin for [PyCharm](https://www.jetbrains.com/pycharm/) / [IntelliJ Platform IDEs](https://www.jetbrains.com/products.html?fromMenu#type=ide) which adds support for [Snakemake](https://snakemake.readthedocs.io/en/stable/) workflows language.

At the moment plugin is an early alpha version and will be improved in future. 

Features available in `*.smk`, `*.rule` and `Snakefile` files:
* Python code syntax highlighting
* Snakemake specific syntax highlighting
* Limited resolve and code completion for python code
* Rules folding

# Installation

If you've already configured custom file type for `Snakefile` or `*.smk` files please **delete** it (**or change** file masks to smth else). Otherwise, your custom file type will replace SnakeCharm support in snakemake files.

In PyCharm (or other `IntelliJ Platfrom` based IDE with `Python Community Edition` plugin installed) open `Preferences|Plugins|Marketplace|SnakeCharm` and press `Install` button.

At the moment the plugin is tested with:
* IDEA CI 2018.3, Python Community Edition Plugin: 2018.3.183.4284.36
* PyCharm 2018.3.1 (Professional Edition), Build #PY-183.4588.64

# IntelliJ Plugins Repository
Plugin is available in IntelliJ Plugins Repository, see [SnakeCharm](https://plugins.jetbrains.com/plugin/11947-snakecharm).

# Support
Contact me at roman.chernyatchik@jetbrains.com or post issues in [Issue Tracker](https://github.com/JetBrains-Research/snakecharm/issues)

# Development
Pull requests are welcome. It is my side project, so I appreciate your help implementing plugin desired features.

FAQ:
* If you get "Unimplemented substep definition" in all `*.feature` files, ensure:
  * Not installed or disabled: `Substeps IntelliJ Plugin` 
  * Plugins installed: `Cucumber Java`, `Gherkin`