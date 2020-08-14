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
      | rule        | input       | directory     | ('')     |
      | rule        | input       | pipe          | ('')     |
      | rule        | input       | protected     | ('')     |
      | rule        | input       | dynamic       | ('')     |
      | rule        | input       | touch         | ('')     |
      | rule        | input       | repeat        | ('')     |
      | rule        | input       | report        | ('')     |
      | rule        | output      | ancient       | ('')     |
      | rule        | output      | unpack        | ('')     |
      | rule        | output      | repeat        | ('')     |
      | checkpoint  | input       | directory     | ('')     |
      | checkpoint  | input       | pipe          | ('')     |
      | checkpoint  | input       | protected     | ('')     |
      | checkpoint  | input       | dynamic       | ('')     |
      | checkpoint  | input       | touch         | ('')     |
      | checkpoint  | input       | repeat        | ('')     |
      | checkpoint  | input       | report        | ('')     |
      | checkpoint  | output      | ancient       | ('')     |
      | checkpoint  | output      | unpack        | ('')     |
      | checkpoint  | output      | repeat        | ('')     |

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
      | rule        | input       | temp('')      |
      | rule        | input       | unpack('')    |
      | rule        | output      | directory('') |
      | rule        | output      | pipe('')      |
      | rule        | output      | protected('') |
      | rule        | output      | dynamic('')   |
      | rule        | output      | touch('')     |
      | rule        | output      | report('')    |
      | rule        | benchmark   | repeat('')    |
      | rule        | benchmark   | protected('') |
      | checkpoint  | input       | ancient('')   |
      | checkpoint  | input       | temp('')      |
      | checkpoint  | input       | unpack('')    |
      | checkpoint  | output      | directory('') |
      | checkpoint  | output      | pipe('')      |
      | checkpoint  | output      | protected('') |
      | checkpoint  | output      | dynamic('')   |
      | checkpoint  | output      | touch('')     |
      | checkpoint  | output      | report('')    |
      | checkpoint  | benchmark   | repeat('')    |
      | checkpoint  | benchmark   | protected('') |