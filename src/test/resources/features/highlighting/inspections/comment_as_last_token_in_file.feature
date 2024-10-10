Feature: Inspection warns about confusing wildcard names.
  Issue 58

  Scenario Outline: Syntax Error
    Given a snakemake project
    Given I set snakemake language version to "1.11.11"
    Given I open a file "foo.smk" with text
        """
        rule all:
            input:
                'foo' <comment_prefix> boo
        """
    And SmkFileEndsWithCommentInspection inspection is enabled
    Then I expect inspection error on <# boo> with message
    """
    Snakemake treats a comment in the end of a file as a syntax error. This behavior was changed in version 7.20.0, but selected version is 1.11.11. Please add a whitespace after the comment or change language version in snakemake settings.
    """
    When I check highlighting errors
    And I invoke quick fix Insert empty line and see text:
    """
    rule all:
        input:
            'foo' <comment_prefix> boo

    """
    Examples:
      | comment_prefix |
      | #              |
      | \n        #    |
      | \n    #        |
      | \n#            |

  Scenario Outline: No Syntax Error 1
    Given a snakemake project
    Given I set snakemake language version to "1.11.11"
    Given I open a file "foo.smk" with text
        """
        rule all:
            input:
                'foo' <comment_prefix> boo
        <last_line>
        """
    And SmkFileEndsWithCommentInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | comment_prefix | last_line |
      | #              |           |
      | \n        #    |           |
      | \n    #        |           |
      | \n#            |           |
      | #              | pass      |
      | #              | a = 2     |

  Scenario: No Syntax Error Other Comments
    Given a snakemake project
    Given I set snakemake language version to "1.11.11"
    Given I open a file "foo.smk" with text
        """
        # comment 1
        rule all:  # comment 2
            # comment 3
        # comment 4
            # comment 5
            input: # comment 6
                # comment 7
            # comment 8
        # comment 9
                'bla' # comment 10
        # comment 11
        pass
        """
    And SmkFileEndsWithCommentInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors

  Scenario: No Syntax Error in non snakemake file
    Given a snakemake project
    Given I set snakemake language version to "1.11.11"
    Given I open a file "file.py" with text
        """
        x = 3

        # comment
        """
    And SmkFileEndsWithCommentInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors


  Scenario Outline: No Syntax Error Fixed Version
    Given a snakemake project
    Given I set snakemake language version to "<version>"
    Given I open a file "foo.smk" with text
        """
        # comment
        """
    And SmkFileEndsWithCommentInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | version  |
      | 7.20.0   |
      | 7.50.2   |
      | 8.2.3    |
      | 9.0.0    |
      | 34.43.43 |
