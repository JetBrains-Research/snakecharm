Feature: Inspection warns min_version specifying version smaller than the one set in settings.
  # Scenarios are pinned to the 'snakemake/utils.py' layout (<= 9.27.0). Later 'snakemake.utils' re-exports
  # 'min_version' from 'snakemake.utils.misc' under 'if sys.version_info >= (3, 11):', which doesn't resolve
  # with the Python 3.7 language level used in tests.

  Scenario Outline: Setting version earlier than one set in min_version causes warning
    Given a <smk_vers> project
    And I set snakemake language version to "<version>"
    And I open a file "foo.smk" with text
    """
    from <min_version_module> import min_version
    min_version("7.6.5")
    """
    When I put the caret at _version(
    # ensure, that `min_version` could be resolved, so inspection is applicable
    Then reference should multi resolve to name, file, times[, class name]
      | min_version | <file_name> | 1 |
    # main check:
    And SmkMinVersionWarningInspection inspection is enabled
    Then I expect inspection weak warning on <min_version("7.6.5")> with message
    """
    Language version specified in settings <version> is earlier than the one specified using min_version. Try changing language version in Snakemake settings
    """
    When I check highlighting weak warnings
    Examples:
      | smk_vers         | min_version_module   | file_name | version |
      | snakemake        | snakemake.utils      | utils.py  | 7.6.4   |
      | snakemake        | snakemake.utils      | utils.py  | 7.4.32  |
      | snakemake        | snakemake.utils      | utils.py  | 6.6.5   |
      | snakemake        | snakemake.utils      | utils.py  | 1.1.1   |
      | snakemake:9.27.0 | snakemake.utils      | utils.py  | 7.6.4   |
      | snakemake:9.27.0 | snakemake.utils      | utils.py  | 1.1.1   |
      | snakemake:10.0.0 | snakemake.utils.misc | misc.py   | 7.6.4   |
      | snakemake:10.0.0 | snakemake.utils.misc | misc.py   | 1.1.1   |

  Scenario Outline: Setting version later than one set in min_version causes no warning
    Given a <smk_vers> project
    And I set snakemake language version to "<version>"
    And I open a file "foo.smk" with text
    """
    from <min_version_module> import min_version
    min_version("7.6.5")
    """
    When I put the caret at _version(
    # ensure, that `min_version` could be resolved, so inspection is applicable
    Then reference should multi resolve to name, file, times[, class name]
      | min_version | <file_name> | 1 |
    # main check:
    And SmkMinVersionWarningInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | smk_vers         | min_version_module   | file_name | version  |
      | snakemake        | snakemake.utils      | utils.py  | 7.6.32   |
      | snakemake        | snakemake.utils      | utils.py  | 7.6.5    |
      | snakemake        | snakemake.utils      | utils.py  | 8.6.4    |
      | snakemake        | snakemake.utils      | utils.py  | 10.10.10 |
      | snakemake:9.27.0 | snakemake.utils      | utils.py  | 7.6.32   |
      | snakemake:10.0.0 | snakemake.utils.misc | misc.py   | 7.6.32   |
      | snakemake:10.0.0 | snakemake.utils.misc | misc.py   | 10.10.10 |

  Scenario Outline: Version without patch number or minor version can be specified
    Given a <smk_vers> project
    And I set snakemake language version to "<version>"
    And I open a file "foo.smk" with text
    """
    from <min_version_module> import min_version
    min_version("<set_version>")
    """
    When I put the caret at _version(
    # ensure, that `min_version` could be resolved, so inspection is applicable
    Then reference should multi resolve to name, file, times[, class name]
      | min_version | <file_name> | 1 |
    # main check:
    And SmkMinVersionWarningInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | smk_vers         | min_version_module   | file_name | set_version | version |
      | snakemake        | snakemake.utils      | utils.py  | 7.6.5       | 7.7     |
      | snakemake        | snakemake.utils      | utils.py  | 7.6.32      | 8       |
      | snakemake        | snakemake.utils      | utils.py  | 8.6.4       | 8.7     |
      | snakemake        | snakemake.utils      | utils.py  | 10.10.10    | 11      |
      | snakemake:9.27.0 | snakemake.utils      | utils.py  | 7.6.5       | 7.7     |
      | snakemake:9.27.0 | snakemake.utils      | utils.py  | 10.10.10    | 11      |
      | snakemake:10.0.0 | snakemake.utils.misc | misc.py   | 7.6.5       | 7.7     |
      | snakemake:10.0.0 | snakemake.utils.misc | misc.py   | 10.10.10    | 11      |
