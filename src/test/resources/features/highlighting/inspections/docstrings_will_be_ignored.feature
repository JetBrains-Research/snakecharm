Feature: Inspection warns that all docstrings will be ignored except the first one.

  Scenario Outline: Weak warning if there are more than one docstring
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like>:
            'docstring 1'
            "docstring 2"
            \"\"\" docstring 3 \"\"\"
            output: "output_file.txt"
        """
    And SmkDocstringsWillBeIgnoredInspection inspection is enabled
    Then I expect inspection weak warning on <"docstring 2"> with message
        """
        All docstrings except the first one will be ignored.
        """
    Then I expect inspection weak warning with message "All docstrings except the first one will be ignored." on
        """
        \"\"\" docstring 3 \"\"\"
        """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

    Scenario Outline: No warning if there is only one docstring or none
      Given a snakemake project
      Given I open a file "foo.smk" with text
        """
        <rule_like>:
            <str>
            output: "output_file.txt"
        """
      And SmkDocstringsWillBeIgnoredInspection inspection is enabled
      And I expect no inspection weak warnings
      When I check highlighting weak warnings
      Examples:
        | rule_like  | str           |
        | rule       | "docstring 1" |
        | rule       |               |
        | checkpoint | "docstring 1" |
        | checkpoint |               |