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
    '<method>' isn't supported in '<section>' section, expected in sections: <expected>.
    """
    When I check highlighting warnings
    Examples:
      | rule_like  | section | method    | arg_list | expected                     |
      | rule       | input   | directory | ('')     | 'output'                     |
      | rule       | input   | pipe      | ('')     | 'output'                     |
      | rule       | input   | protected | ('')     | 'benchmark', 'log', 'output' |
      | rule       | log     | temp      | ('')     | 'input', 'output'            |
      | rule       | input   | dynamic   | ('')     | 'output'                     |
      | rule       | input   | touch     | ('')     | 'output'                     |
      | rule       | input   | repeat    | ('')     | 'benchmark'                  |
      | rule       | input   | report    | ('')     | 'output'                     |
      | rule       | output  | ancient   | ('')     | 'input'                      |
      | rule       | output  | unpack    | ('')     | 'input'                     |
      | checkpoint | input   | directory | ('')     | 'output'                     |
      | checkpoint | input   | pipe      | ('')     | 'output'                     |
      | checkpoint | input   | protected | ('')     | 'benchmark', 'log', 'output' |
      | checkpoint | log     | temp      | ('')     | 'input', 'output'            |
      | checkpoint | input   | dynamic   | ('')     | 'output'                     |
      | checkpoint | input   | touch     | ('')     | 'output'                     |
      | checkpoint | input   | repeat    | ('')     | 'benchmark'                  |
      | checkpoint | input   | report    | ('')     | 'output'                     |
      | checkpoint | output  | ancient   | ('')     | 'input'                      |
      | checkpoint | output  | unpack    | ('')     | 'input'                     |

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


  Scenario Outline: Complex cases not be confused
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
      | rule        | output      | foo.ancient('')   |
      | rule        | output      | ancient.foo('')   |