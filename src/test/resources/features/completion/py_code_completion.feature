Feature: Completion in python part of snakemake file

  Scenario: Complete any python method
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    foo = 1;
    """
    When I put the caret after foo = 1;
    And I invoke autocompletion popup
    Then completion list should contain:
      | print |

  Scenario Outline: Issue 8
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def fa_files(wildcards):
        return ','.join(glob("{}/*.fa".format(config.fa_dir)))
    """
    When I put the caret at <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <option> |
    Examples:
      | signature             | option |
      | format(config.fa_dir) | format |
      | join(glob             | join   |
