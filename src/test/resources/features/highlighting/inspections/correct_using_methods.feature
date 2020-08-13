Feature: Inspection for methods from snakemake library

  Scenario Outline: Incorrect using ancient/protected/directory methods
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
       <section>: <method><arg_list>
    """
    And SmkMisuseUsageIOFlagMethodsInspection inspection is enabled
    Then I expect inspection warning on <<method><arg_list>> in <<section>: <method><arg_list>> with message
    """
    '<method>' isn't supported in '<section>'
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section     | method        | arg_list |
      | rule        | input       | protected     | ('')     |
      | rule        | input       | directory     | ('')     |
      | rule        | output      | ancient       | ('')     |
      | rule        | log         | ancient       | ('')     |
      | rule        | log         | directory     | ('')     |
      | rule        | benchmark   | ancient       | ('')     |
      | rule        | benchmark   | directory     | ('')     |
      | rule        | log         | temp          | ('')     |
      | rule        | benchmark   | temp          | ('')     |
      | rule        | input       | report        | ('')     |
      | rule        | log         | report        | ('')     |
      | rule        | benchmark   | report        | ('')     |

  Scenario Outline: Correct using ancient/protected/directory methods
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
       <section>: <method>
    """
    And SmkMisuseUsageIOFlagMethodsInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | rule_like   | section     | method        |
      | rule        | input       | ancient('')   |
      | rule        | benchmark   | protected('') |
      | rule        | output      | protected('') |
      | rule        | log         | protected('') |
      | rule        | output      | directory('') |
      | rule        | input       | temp('')      |
      | rule        | output      | temp('')      |
      | rule        | output      | report('')    |