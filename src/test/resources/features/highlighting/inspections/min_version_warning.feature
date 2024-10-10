Feature: Inspection warns min_version specifying version smaller than the one set in settings.

  Scenario Outline: Setting version earlier than one set in min_version causes warning
    Given a snakemake project
    And I set snakemake language version to "<version>"
    And I open a file "foo.smk" with text
    """
    from snakemake.utils import min_version
    min_version("7.6.5")
    """
    When I put the caret at _version(
    # ensure, that `min_version` could be resolved, so inspection is applicable
    Then reference should multi resolve to name, file, times[, class name]
      | min_version | utils.py | 1 |
    # main check:
    And SmkMinVersionWarningInspection inspection is enabled
    Then I expect inspection weak warning on <min_version("7.6.5")> with message
    """
    Language version specified in settings <version> is earlier than the one specified using min_version. Try changing language version in Snakemake settings
    """
    When I check highlighting weak warnings
    Examples:
      | version |
      | 7.6.4   |
      | 7.4.32  |
      | 6.6.5   |
      | 1.1.1   |

  Scenario Outline: Setting version later than one set in min_version causes no warning
    Given a snakemake project
    And I set snakemake language version to "<version>"
    And I open a file "foo.smk" with text
    """
    from snakemake.utils import min_version
    min_version("7.6.5")
    """
    When I put the caret at _version(
    # ensure, that `min_version` could be resolved, so inspection is applicable
    Then reference should multi resolve to name, file, times[, class name]
      | min_version | utils.py | 1 |
    # main check:
    And SmkMinVersionWarningInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | version  |
      | 7.6.5    |
      | 7.6.32   |
      | 8.6.4    |
      | 10.10.10 |

  Scenario Outline: Version without patch number or minor version can be specified
    Given a snakemake project
    And I set snakemake language version to "<version>"
    And I open a file "foo.smk" with text
    """
    from snakemake.utils import min_version
    min_version("<set_version>")
    """
    When I put the caret at _version(
    # ensure, that `min_version` could be resolved, so inspection is applicable
    Then reference should multi resolve to name, file, times[, class name]
      | min_version | utils.py | 1 |
    # main check:
    And SmkMinVersionWarningInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | set_version | version |
      | 7.6.5       | 7.7     |
      | 7.6.32      | 8       |
      | 8.6.4       | 8.7     |
      | 10.10.10    | 11      |