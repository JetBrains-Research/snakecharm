<!-- Keep a Changelog guide -> https://keepachangelog.com -->
<!-- See https://github.com/JetBrains/gradle-changelog-plugin -->

# SnakeCharm Plugin Changelog

## [2025.2.1]
Released on <Unreleased>

### Plugin
- Compatibility with PyCharm 2025.2 (see [#563](https://github.com/JetBrains-Research/snakecharm/issues/563)
- Tests fail on CI with error: File accessed outside allowed roots error for project testdata (see [#565](https://github.com/JetBrains-Research/snakecharm/issues/565)

### Fixed
- Wrappers docs url is invalid for wrappers version >= 6.x  (see [#564](https://github.com/JetBrains-Research/snakecharm/issues/564)

### Changed
- Bundled snakemake wrappers list updated to `v7.2.0`
- Revised snakemake language level support up to the 9.9.0 release
- Inspections:
    - For snakemake language level >= 9.5.0 allow wildcards/callable functions fo `container:` section

## [2025.1.1]
Released on 18 April 2025

### Fixed
- Compatibility with PyCharm 2025.1 (see [#560](https://github.com/JetBrains-Research/snakecharm/issues/560)
- Some code insight features stopped working with Snakemake 9.x release (see [#559](https://github.com/JetBrains-Research/snakecharm/issues/559)


## [2024.2.3]
Released on 5 November 2024

### Fixed
- Wildcard is not properly defined false positive warning when typing first input, then output section (see [#557](https://github.com/JetBrains-Research/snakecharm/issues/557)


## [2024.2.2]
Released on 24 October 2024

NB: Please configure/ensure the desired snakemake language level in the plugin settings, by default, it will be 8.24.0 in this release. Language level setting is required to show Snakemake API that is deprecated in the desired Snakemake version or not yet available. See example screenshots in [#508](https://github.com/JetBrains-Research/snakecharm/issues/508).

### Features
- Inspection: Warn about deprecated or not yet available features based on snakemake language level (see [#508](https://github.com/JetBrains-Research/snakecharm/issues/508)
- Move snakemake API addition/removal info from JAR into `extra/snakemake_api.yaml` in plugin directory
- Inspection: Warn if snakemake section type or API function isn't supported in the current snakemake project based on a snakemake version (see [#500](https://github.com/JetBrains-Research/snakecharm/issues/500)
- Code completion: For section keywords show 'since' version and deprecation notice in the completion list. Do not suggest already removed keywords (see [#535](https://github.com/JetBrains-Research/snakecharm/issues/535)
- Set default project Snakemake language level in Snakemake support settings to 8.24.0
- [8.3.0] Support for: lookup, evaluate, branch, collect, exists (see [#548](https://github.com/JetBrains-Research/snakecharm/issues/548)
- [8.0.0] Support for Global workflow dependencies [8.0.0] syntax (see [#555](https://github.com/JetBrains-Research/snakecharm/issues/555)
- [8.0.0-6.8.1] Support for: 'storage', 'github', 'gitfile', 'gitlab' (see [#550](https://github.com/JetBrains-Research/snakecharm/issues/550)
- [7.25.0] Support for localrule directive (see [#524](https://github.com/JetBrains-Research/snakecharm/issues/524)
- [7.11] Resource scopes support (see [#510](https://github.com/JetBrains-Research/snakecharm/issues/510)

### Fixed
- Improve parser error message when rule/module is declared with name but lacks ':' (see [#515](https://github.com/JetBrains-Research/snakecharm/issues/515))
- Support for `update` and `before_update` flags. Update inspection that warns if flag functions from `snakemake.io` is used in a wrong section, added info for all flags up to 8.23.1 version (see [#537](https://github.com/JetBrains-Research/snakecharm/issues/537))
- Inject some modules in Snakefile file resolve scope w/o import declaration, e.g. os, sys,.. (see [#553](https://github.com/JetBrains-Research/snakecharm/issues/553)
- Do not warn about unresolved `snakemake` variable in the python files, because they could be used as scripts/wrappers for snakemake rules.  (see [#511](https://github.com/JetBrains-Research/snakecharm/issues/511))
- Undeclared section `threads` warning should not be shown here (see [#539](https://github.com/JetBrains-Research/snakecharm/issues/539)

## [2024.2.1]
Released on 9 October 2024

### Fixed
- Compatibility with PyCharm 2024.2-2024.3 EAPs (see [#532](https://github.com/JetBrains-Research/snakecharm/issues/532))
- Improve the parser error message when rule/module is declared with name but lacks ':' (see [#515](https://github.com/JetBrains-Research/snakecharm/issues/515))
- For some wrappers the documentation popup shows the wrong description (e.g. `bio/bwa/mem-samblaster` for `bio/bwa/mem`)
- Bundled snakemake wrappers list updated to `v4.6.0`
- Use SDK chooser w/o mange sdks button on the Snakemake Language Frameworks Settings page. The previous version used legacy API that doesn't manage SDKs correctly.   
- Exceptions: EA-655, EA-220, EA-100, EA-40

### Changed
- Switch plugin build scripts to `intellij-gradle-plugin` 2.1.0 API version (required for 2024.2+ PyCharm) 
- Move cached wrappers info out of plugin JAR file to `../extra` directory 

## [2024.1.1]
Released on 20 April 2024

### Fixed
- Compatibility with PyCharm 2024.1
- Bundled snakemake wrappers list updated to `v3.8.0`
- Completion for wrapper names rarely works (see [#517](https://github.com/JetBrains-Research/snakecharm/issues/517))

## [2023.3.1]
Released on 6 Dec 2023 (EAP on 3 Nov 2023)

### Fixed
- Compatibility with PyCharm 2023.3 (see [#507](https://github.com/JetBrains-Research/snakecharm/issues/507))
- Exceptions: EA-232

### Changed:
- Bundled snakemake wrappers list updated to `v2.9.1`

## [2023.2.1]
Released on 27 Jul 2023

### Fixed
- Compatibility with PyCharm 2023.2

### Changed:
- Bundled snakemake wrappers list updated to `v2.2.1`

## [2023.1.1]
Released on 9 April 2023

### Fixed
- Compatibility with PyCharm 2023.1 #505  (see [#505](https://github.com/JetBrains-Research/snakecharm/issues/505))
- Exception: "Access is allowed from write thread only" creating project in dumb mode (see [#506](https://github.com/JetBrains-Research/snakecharm/issues/506)) 
- Make Snakecharm trusted plugins, even though it is provided not by JetBrains but by JetBrains Research (see [#503](https://github.com/JetBrains-Research/snakecharm/issues/503))
- Empty `What's New` section fixed

### Changed:
- Bundled snakemake wrappers list updated to `v1.25.0`
- Plugin versioning changed, now minor version is not CI build, just manual counter 

## [2022.3.771]
Released on December 7th 2022

### Fixed
- Compatibility with PyCharm 2022.3 #501  (see [#501](https://github.com/JetBrains-Research/snakecharm/issues/501))

### Changed:
- Bundled snakemake wrappers list updated to `v1.20.0`

## [2022.2.761]
Released on August 5th 2022

### Fixed
- Compatibility with PyCharm 2022.2
- Ignore whitespaces that could occur after `as` in `use` block 
- Bundled wrappers repo updated to v1.7.1
                                          
### Added:
- [7.10.0] Support conda environment definitions to be passed as function pointers, similar to input, params, and resources #494  (see [#494](https://github.com/JetBrains-Research/snakecharm/issues/494)) 
- [7.9.0] Syntax supprt for `exclude` keyword in use rules (see [#495](https://github.com/JetBrains-Research/snakecharm/issues/495), [#496](https://github.com/JetBrains-Research/snakecharm/issues/496)) 
- [ 7.0.0] template_engine: basic support (see [#497](https://github.com/JetBrains-Research/snakecharm/issues/497))
  - Do not highlight `template_engine` as unrecognized section
  - Inspections:
    - Only one argument expected after `template_engine` 
    - Keywords arguments not supported after `template_engine` 
    - Do not warn about unused log section if rule with `template_engine` 
    - `template_engine` is execution section, so could not be with run/shell/notebook/script 
    - `template_engine` should be last in the rule 
    - Completion for "yte", "jinja2" in template_engine string argument 
    - Inspection that warns when template= keyword argument is required in section with template_engine

## [2022.1.749]
Released on June 15th 2022
                                          
### Fixed 
- "Snakemake support is disabled. Not all features are available" is show in non Snakemake context  (see [#491](https://github.com/JetBrains-Research/snakecharm/issues/491))
- Do not show error for conda env that looks like already existing env (see [#493](https://github.com/JetBrains-Research/snakecharm/issues/493)) 
       
### Added:
- "default_target:" directive support  (see [#473](https://github.com/JetBrains-Research/snakecharm/issues/473)) 
- "retries:" directive support  (see [#490](https://github.com/JetBrains-Research/snakecharm/issues/490)) 
- "ensure:" directive support  (see [#489](https://github.com/JetBrains-Research/snakecharm/issues/489)) 

## [2022.1.743]
Released on May 4th 2022
                                          
### Changed:
- Plugin is not compatible with PyCharm 2021.x due to 2022.1 API changes
- Bundles snakemake wrappers list updated to `v1.3.2`

### Fixed
- Make plugin compatible with PyCharm 2022.1  (see [#487](https://github.com/JetBrains-Research/snakecharm/issues/487))
- Resolve/completion for checkpoints after `rules` keyword (see [#262](https://github.com/JetBrains-Research/snakecharm/issues/262))
- Quick fix for unused log file inspection (see [#452](https://github.com/JetBrains-Research/snakecharm/issues/452))
- Resolve for rule names in `use rule` section (see [#455](https://github.com/JetBrains-Research/snakecharm/issues/455))
- Multiple args inspection in `workdir` case (see [#140](https://github.com/JetBrains-Research/snakecharm/issues/140))
- `localrules` and `ruleorder` now take into account `use rule` (see [#448](https://github.com/JetBrains-Research/snakecharm/issues/448))
- Keyword arguments highlighting (see [#454](https://github.com/JetBrains-Research/snakecharm/issues/454))
- Resolve for `rules` keyword if `snakemake` version less than `6.1` (see [#359](https://github.com/JetBrains-Research/snakecharm/issues/359))
- Plugin Logo Updated  (see [#449](https://github.com/JetBrains-Research/snakecharm/issues/449)) 
- Snakemake file type icon updated 
- 'copy-minimal' shadow section missing in completion (see [#467](https://github.com/JetBrains-Research/snakecharm/issues/467)) 

### Added
- Inspection: warns that that all docstrings except the first one will be ignored (see [#341](https://github.com/JetBrains-Research/snakecharm/issues/341))
- Quick fix for unresolved files (conda, configfile. etc.) (see [#277](https://github.com/JetBrains-Research/snakecharm/issues/277))
- Gutter icons for rule inheritance (see [#429](https://github.com/JetBrains-Research/snakecharm/issues/429))
- Warning: Snakemake support is disabled (see [#109](https://github.com/JetBrains-Research/snakecharm/issues/109))

## [2021.3.661]
Released on December 7th 2021

### Changed
- PyCharm 2021.3 compatibility (see [#444](https://github.com/JetBrains-Research/snakecharm/issues/444))
  Released on December 7th 2021

## [2021.2.657]
Released on December 6th 2021

### Changed
- Show argument preview for int indexes completion in `output[i]` (see [#378](https://github.com/JetBrains-Research/snakecharm/issues/378))
- Allocation of all top-level sections into one token and support for unknown top-level sections (see [#373](https://github.com/JetBrains-Research/snakecharm/issues/373))

### Fixed
- Resolve and completion for 'script' section (see [#426](https://github.com/JetBrains-Research/snakecharm/issues/426))
- Only snakemake file can't end with a comment (see [#365](https://github.com/JetBrains-Research/snakecharm/issues/365))
- SmkSectionDuplicatedArgsInspection now checks workflow top-level sections (see [#407](https://github.com/JetBrains-Research/snakecharm/issues/407))
- All inspections warnings now can be overridden (see [#423](https://github.com/JetBrains-Research/snakecharm/issues/423))
- Completion for .smk files in module 'snakefile' section (see [#428](https://github.com/JetBrains-Research/snakecharm/issues/428))
- Default path "config/config.yaml" now supported (see [#363](https://github.com/JetBrains-Research/snakecharm/issues/363))
- Access by index to input/output sections with 'multiext' function (see [#278](https://github.com/JetBrains-Research/snakecharm/issues/278))
- Inspection: Do not show warning that rule section is unused variable if rule is defined in method (see [#385](https://github.com/JetBrains-Research/snakecharm/issues/385))
- SOE: From UnusedLocal inspection for SnakemakeSL expression  (see [#380](https://github.com/JetBrains-Research/snakecharm/issues/380))
- Do not show syntax error for rule w/o body (see [#420](https://github.com/JetBrains-Research/snakecharm/issues/420))

### Added
- Color Settings Page (see [#431](https://github.com/JetBrains-Research/snakecharm/issues/431))
- Inspection: highlights 'use rule' section which overrides several rules as one (see [#411](https://github.com/JetBrains-Research/snakecharm/issues/411))
- Collecting wildcards from overridden rules in 'use' sections (see [#418](https://github.com/JetBrains-Research/snakecharm/issues/418))
- Weak warnings for unused 'log' sections in 'use rule' (see [#414](https://github.com/JetBrains-Research/snakecharm/issues/414))
- Weak warnings for unused 'log' sections (see [#300](https://github.com/JetBrains-Research/snakecharm/issues/300))
- Support for 'module' and 'use' keywords (see [#355](https://github.com/JetBrains-Research/snakecharm/issues/355))
- Inspection for improperly called functions (see [#148](https://github.com/JetBrains-Research/snakecharm/issues/148))
- Ability for memorising new section name (see [#372](https://github.com/JetBrains-Research/snakecharm/issues/372))
- Support for 'handover' section (see [#362](https://github.com/JetBrains-Research/snakecharm/issues/362))
- Support for 'containerized' section (see [#361](https://github.com/JetBrains-Research/snakecharm/issues/361))
- Inspection: Show ERROR for execution sections in 'use' section (see [#408](https://github.com/JetBrains-Research/snakecharm/issues/408))
- Completion features related to 'use' section (see [#413](https://github.com/JetBrains-Research/snakecharm/issues/413))
- Add reference for 'snakefile:' in module declaration (see [#409](https://github.com/JetBrains-Research/snakecharm/issues/409))
- Initial PEPs support (see [#360](https://github.com/JetBrains-Research/snakecharm/issues/360))

## [2021.2.424]
Released on July 5th 2021

### Changed
- Plugin structure changed to [Developing plugins using GitHub Template](https://plugins.jetbrains.com/docs/intellij/github-template.html) (see [#358](https://github.com/JetBrains-Research/snakecharm/issues/358))

### Fixed
- Slow operations are prohibited on EDT (see [#366](https://github.com/JetBrains-Research/snakecharm/issues/366))
- Compatibility with Snakemake 5.6.x (see [#367](https://github.com/JetBrains-Research/snakecharm/issues/367))

## [2021.2.418]
Released on June 22th 2021

### Changed
- Compatible with [PyCharm](https://www.jetbrains.com/pycharm/) `2021.1.1` - `2021.3` EAPs, [DataSpell](https://www.jetbrains.com/dataspell/) EAPs
- Bundled meta-data for snakemake-wrappers (repo version: `0.75.0`)

## [2021.2.414]
Released on May 3th 2021

### Added
* Compatible with [PyCharm](https://www.jetbrains.com/pycharm/) 2021.1.1 - 2021.2 EAPs, [DataSpell](https://www.jetbrains.com/dataspell/) EAPs

Version number now represents target PyCharm/DataSpell/IntelliJ platform major version for `2021.2.0.414`:
* `2021.2`  - intellij platform version
* `.414` - plugin build number
* `-eap.2` - beta version 2

## [0.11.411]
Version 0.11.0, build #411, released on April 24th 2021
  
### Changed:
- NB: Now Snakemake support is enabled only if Snakemake framework support is selected in
    * PyCharm: `Settings | Languages & Frameworks | Snakemake | Enable Snakemake Support`
    * IDEA: Add `Snakemake` facet

- It is possible to use different Python Interpreters for your project and for Snakemake features. 
  E.g. you could have a conda environment with all packages required for scripts used in rules, but 
  without snakemake module itself.

### Added
- Support PyCharm 2021.1 build (see #350)
- Snakemake Support Settings page (see #346)
- Support any section names syntax in rules, checkpoints, subworkflows (see #334)
- Support `name:` section argument syntax (see #351)
- Experimental wrappers smart features (thanks to @SimonTsirikov for help):
    - Bundled meta-data for snakemake-wrappers (repo version: 0.73.0)
    - Snakemake wrappers names resolve/completion (see #18)
    - Rule section keyword arguments completion based on wrappers info (see #18)
    - Wrapper params are taken from 'wrappers.py', 'wrappers.r', 'meta.yaml' (see #324)

### Fixed
- Inspection marks 'rules.' as unresolved in snakemake >= 6.1 (see #352)
- Inspections:
    - Weak warning about sections which are not recognized by SnakeCharm (see #334)

## [0.9.387]
Version 0.9.0, build #387, released on Dec 16th 2020

Special thanks to our contributors Simon Tsirikov (@SimonTsirikov) and Artem Davletov (@ArtemDavletov). Without their help this major plugin
update won't be possible.

### Added
- PyCharm 2020.3 compatibility (see #337)
- Snakemake specific formatter settings (see #338)
- `envmodules` parsing (see #332)
- Inspection: `input`, `output` can't be used in `params` sections directly (see #153)
- Inspection: Comment at the end of Snakefile is treated as syntax error (see #58)
- Inspection: envmodules directive is only allowed with shell, script, notebook, or wrapper directives (not with run) (see #340)
- Inspection: Warn about double quotation on runtime in cases like {config['foo']} for snakemake string injections (see #329)
- Inspection: Warn users if they have arguments with same value in section (see #263)
- Inspection: Comma in the end of section arg list isn't needed (see #255)
- Inspection: Warn user if rule from 'rulesorder' isn't available in current or included files (see #254)
- Inspection: Warn about string arguments split on several lines (see #259)
- Injection: Inject regexp language into wildcard_constraints section params values (see #182)
- Inspection: Correct using methods ancient, protected, directory (see #250)
- Inspection: Warn users if they have callable arguments in sections that does not expect it (see #198)
- Inspection: Warn users if they have keyword arguments in sections that does not expect it (see #196)
- Inspection: Warn about usage of rule/checkpoint/subworkflow objects as arguments instead of one of its fields (see #257)
- Inspection: Warn about missed arguments needed for wrapper (see #18)
- Resolve\completion for wrapper path and for arguments that wrapper use (see #18)
- Wrapper section documentation popup (see #18)
- Action: Correct Up/Down StatementMover for rule sections (see #174)
- Action: Show folding for multiline rule sections (input, output, etc) (see #314)

### Fixed
- PluginException: Inspection #SmkAvoidTabWhitespace has no description [Plugin: SnakeCharm]
- Do not warn about 'touch' misuse in benchmark and log sections
- Completion does not suggest top-level keywords in comments (see #178)
- Suppress inspection doesn't work in some cases (see #313)
- Don't show multiline string arg, may be comma is missing for sections with single arg (see #315)
- Expected `collections.iterable` but got `output:` false positive (see #317)
- Expected type `Iterable` (..), git `input: ` instead (see #319)
- Expected type `Iterable` (..), git `input: ` instead (see #320)
- Workaround for: 'Shadows built-in name `input`' inspection on lambda parameters (see #133)
- Confusing 'Cannot check missing wildcards here.' warning (see #307)

## [0.8.214]
Version 0.8.0, build #214, released on July 3th 2020

### Added
- PyCharm 2020.2 builds compatibility


## [0.7.209]
Version 0.7.0, build #209, released on July 2th 2020

### Added
- Inspection: warn about confusing reference in localrules section (see #287)
- Inspection: Warn if mixed tabs and spaces (see #285)
- 'notebook' section parsing error (see #288)
- `envvars` isn't parsed properly (see #281)
- `container` top level and rule sections parsing (#291)
- Resolve/completion for paths in notebook section (see #289)
- Inspection: Conda environments are only allowed with shell, script, notebook,
    or wrapper directives (not with run). #290

### Fixed
- Rule not yet defined false positive for lambdas (see #286)
- Unresolved reference for 'rule' in 'run' section (see #283)
- Doesn't automatically create folding code element for "checkpoint" (see #273)
- Multiline shell commands in anonymous rules defined in loops (see #275)
- Do not warn about missing wildcards if no SmkSL injection in string (see #266)

## [0.6.165]
Version 0.6.0, build #165, released on April 12th 2019

### Fixed
- 2020.1 Compatibility (see #279)
- “cache:” rule section support (see #280)

## [0.5.126]
Version 0.5.2, build #126, released on November 26th 2019

### Fixed
- UnsupportedOperationException (see #271)

## [0.5.124]
Version 0.5.2, build #124, released on November 26th 2019
         
### Changed
- Minor update compatible with 2019.3 builds

### Fixed
- 2019.2 Deprecated API cleanup (see #219)
- PluginException: While loading class SnakemakeVisitorFilter (see #270)
- PsiInvalidElementAccessException: PyUnboundLocalVariableInspection (see #268)


## [0.5.126-0]
Version 0.5.1, build #126, released on November 26th 2019

### Changed
- Minor update which corrects untilBuild, this plugin version isn't compatible with 2019.3 EAP / RC.

### Fixed
- SnakeCharm is not compatible with AppCode (see #269)


## [0.5.112]
Version 0.5, build #112, released on November 20th 2019 (removed from plugins repo)

Special thanks to our contributors Sharkova Darya and Nazarov Nikita. Without them this major plugin
update won't be possible.

#### Changed:
- sinceBuild "183.4284" (2018.3)
- untilBuild "193.*" (2019.3)
- To get all features enabled please configure in your project Python interpreter with snakemake
module installed. Otherwise some smart code insight features will not work.

### Added
- Wildcards support with syntax highlighting, code completion, resolve, find usages,
    rename and various useful inspections
- Internal code parser and lexer updated to support multiline strings and removes wrong
    errors highlighting in many places
- Code completion / Resolve Features
    - SnakemakeSL language: implement basic type inference & resolve/completion engine, see #183
    - Completion/Resolve for wildcards, output, input in lambdas, see #230
    - Includes links resolve and completion, see #20
    - File reference support for `conda` section, see #144
    - In rules/checkpoint completion: show all names only on 2nd auto-completion invocaton, see #173
    - ruleorder, localrules should resolve/complete not only rules from same file, but all rules from project, see #155
    - Completion in 'run:' section for input/output/log/resources/wildcards/params keys, #237
    - Python variables completion in SmkSL injections, #213
    - Resolve / completion for indexes in subscription expressions, e.g. output[1], #243
    - Show all project rules/checkpoints in autocompletion, #120
    - 'shell' section & call: resolve & completion for input args, #152
    - Resolve completion for `input.foo` in lambda functions, #154
    - Resolve/Completion for: input, output, params rule options in shell argument, #3
- Inspections:
    - Support highlighting for arguments of format strings, see #53
    - Duplicated keyword arguments, see #72
    - Multiple arguments inspection doesn't work for several keywords, see #159
    - Inspection quick fixes, see #194
    - False position arg after keyword arg inspection warning, see #210
    - Highlight wildcard name after `wildcards.foo` same as in injection '{foo}', see #227
    - Inspection: warn if wildcards set is different in input, output, log, message and
     other required sections (check snakemake sources & runtime behaviour), see #207
    - Inspections for identifier names in SnakemakeSL, #215
    - Inspection for unavailable rule sections in SnakemakeSL, #199
    - Inspection: warn if wildcard name is same as section name, #244
    - Warn about section argument index out of bounds exception , #245
    - Warn if lambda argument isn't supported for the section, #141
    - Inspection: SyntaxRuntime Error: Only input files can be specified as functions, #156
    - Warn if user uses dot in wildcards names, it is confusing, #265
    - Highlight wildcards in python code, #264
    - Inspection: wildcards in shell script can be used only with `wildcards.` prefix., #56
- Find Usages / Rename:
    - For rule sections and wildcards, #233
    - For rules, checkpoints
- Other:
    - Determine wildcard names for rule, see #184
    - Multiline string syntax error not reported, see #146
    - Implemented brace matcher for SmkString injection language, see #181
    - Inject Snakemake format language in strings concatenated by `+`, see #189
    - Do not inject SmkSl in rule parameter lambda functions, see #204
    - Do not inject SmkSL in f-strings without { or } in string part, see #201
    - Lambda expression cannot be used in some rule section, see #187
    - Parsing of python format specifiers, see #214
    - Inject SmkSL in os.path.join() args, #228
    - Don't detect wildcards in snakemake expand() method, #229
    - Don't resolve/complete rule like names when not part of 'rules.' or 'checkpoints.' reference, #223
    - Do not collect rule wildcards from lambdas, #249

### Fixed
- Parsing error for rules after certain Python statements, see #130
- Top level workflow sections issue, see #179
- F-strings parsing errors in onstart section, see #200
- Wrong completion list after rules./checkpoints., see #211
- Ignore rule redeclaration in conditional statements, #241
- Do not warn: Parameter 'cmd' unfilled in 'shell', #242
- Cannot find reference 'get' in section, #234
- No warning if rule name already used in other file, #253

##[0.4.46]
Version 0.4, build #46, released on August 7th 2019

#### Changed:
- sinceBuild "183.4284" (2018.3)
- untilBuild "192.*" (2019.2)

### Fixed
- NoSuchMethodException: com.jetbrains.python.parsing.ExpressionParsing.parseFormattedStringNode()
    when using PyCharm Professional or IntelliJ IDEA Ultimate, see #168 (thanks to @winni2k)

##[0.4.42]
Version 0.4, build #42, released on July 30th 2019

#### Changed:
- sinceBuild "183.4284" (2018.3)
- untilBuild "192.*" (2019.2)

### Added
-  Resolve/Completion for paths in after "include:", see #20

### Fixed
- Minor bug fixes


##[0.4.35]
Version 0.4, build #35, released on July 25th 2019

#### Changed:
- sinceBuild "183.4284" (2018.3)
- untilBuild "192.*" (2019.2)

### Added
- Completion for `shadow` level values, see #12
- Shadow settings documentation popup, see #73
- Code completion for subworkflow sections names, see #11
- Resolve/Completion for subworflows names, see #60
- Subworkflows support, see #90
- Snakecharm incompatible with current PyCharm version, see #143
- Support method lines separators for snakemake rules, see #149
- Inspection: `shadow` level values, see #65
- Inspection: trailing comma at the end of Snakefile major parsing, see #58
- Inspection: Multiple declarations of the same section inspections, see #71
- Inspection: Keyword arguments duplication inspections, see #72
- Inspection: Subworflow redeclaration, see #88
- Inspection: subworkflow 'workdir', 'snakefile', 'configfile' could accept only single arg, see #83
- Inspection: Warn if rule mentioned more than 1 time in ruleorder / localrule, see #107

### Fixed
- NoClassDefFoundError: com/intellij/psi/util/PsiTreeUtilKt, see #70
- Rule docstrings trigger a syntax error, see #31
- ClassCastException: kotlin.jvm.internal.ClassReference cannot be cast to
 kotlin.jvm.internal.ClassBasedDeclarationContainer, see #68
- 'input' section name autocompletion results in ": input" insertion, see #69
- Subworkflow parsing errors major parsing subtask, see #66
- Parsing error when new line begins with a separator in argument list, see #96
- Multiline strings declaration support, see #16

##[0.3.0]
Version 0.3, released on June 23th 2019 (eap channel)

#### Changed:
- sinceBuild "183.4284"
- untilBuild "191.*"

### Added
- Completion/resolve for methods like 'touch', 'expand', etc., also `rules`, `config` objects, see #1
- Completion/resolve for rules names after 'rules.'
- Code inspections:
    - Multiple run or shell keywords inspections, #48
    - A rule name is already used by another rule, #17
    - A positional argument follows a keyword argument
    - No rule keywords allowed after run/shell/script/wrapper/cwl in rule foo.', #37

##[0.2.0]
Version 0.2, released on Feb 4th 2019

#### Changed:
- sinceBuild "183.4284"
- untilBuild "191.*"
    
### Fixed
- *.smk file not recognized as Snakemake files, see issue #28
- *.rules files support, see issue #21
- Do not parse/highlight as keywords several top level workflow params, see issue #29
- Improved code indentation in rules, no line continuation is inserted, but indentation
  is relative to ':', see issue #26
- Rules and rule params folding support #15
