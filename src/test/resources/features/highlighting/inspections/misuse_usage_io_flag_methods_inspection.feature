Feature: Inspection for methods from snakemake library

  Scenario Outline: Incorrect using flag methods in 8.7.0
    Given a snakemake project
    And I set snakemake language version to "8.7.0"
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
      | rule_like | section | method     | arg_list | expected |
      | rule      | input   | update     | ('')     | 'output' |
      | rule      | output  | from_queue | ('')     | 'input'  |

  Scenario Outline: Incorrect using flag methods
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
      | rule       | log     | temp      | ('')     | 'output'                     |
      | rule       | input   | touch     | ('')     | 'benchmark', 'log', 'output' |
      | rule       | input   | repeat    | ('')     | 'benchmark'                  |
      | rule       | input   | report    | ('')     | 'output'                     |
      | rule       | input   | ensure    | ('')     | 'output'                     |
      | rule       | output  | ancient   | ('')     | 'input'                      |
      | rule       | output  | unpack    | ('')     | 'input'                      |
      | checkpoint | input   | directory | ('')     | 'output'                     |
      | checkpoint | input   | pipe      | ('')     | 'output'                     |
      | checkpoint | input   | protected | ('')     | 'benchmark', 'log', 'output' |
      | checkpoint | log     | temp      | ('')     | 'output'                     |
      | checkpoint | input   | touch     | ('')     | 'benchmark', 'log', 'output' |
      | checkpoint | input   | repeat    | ('')     | 'benchmark'                  |
      | checkpoint | input   | report    | ('')     | 'output'                     |
      | checkpoint | output  | ancient   | ('')     | 'input'                      |
      | checkpoint | output  | unpack    | ('')     | 'input'                      |
      # not resolved:
      | rule       | input   | dynamic   | ('')     | 'output'                     |
      | checkpoint | input   | dynamic   | ('')     | 'output'                     |

  Scenario Outline: Incorrect using flag methods before 8.0
    # dynamic was removed in 8.0 and feature is based on resolve

    Given a snakemake:7.32.4 project
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
      | rule       | input   | dynamic   | ('')     | 'output'                     |
      | checkpoint | input   | dynamic   | ('')     | 'output'                     |

  Scenario Outline: Correct using flag methods
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
      | rule_like  | section   | method         |
      | rule       | input     | ancient('')    |
      | rule       | output    | temp('')       |
      | rule       | input     | unpack('')     |
      | rule       | input     | from_queue('') |
      | rule       | output    | directory('')  |
      | rule       | output    | pipe('')       |
      | rule       | output    | protected('')  |
      | rule       | output    | dynamic('')    |
      | rule       | output    | touch('')      |
      | rule       | output    | ensure('')     |
      | rule       | log       | touch('')      |
      | rule       | benchmark | touch('')      |
      | rule       | output    | report('')     |
      | rule       | benchmark | repeat('')     |
      | rule       | benchmark | protected('')  |
      | rule       | output    | update('')     |
      | checkpoint | input     | ancient('')    |
      | checkpoint | output    | temporary('')  |
      | checkpoint | input     | unpack('')     |
      | checkpoint | output    | directory('')  |
      | checkpoint | output    | pipe('')       |
      | checkpoint | output    | protected('')  |
      | checkpoint | output    | dynamic('')    |
      | checkpoint | output    | touch('')      |
      | checkpoint | output    | report('')     |
      | checkpoint | benchmark | repeat('')     |
      | checkpoint | benchmark | protected('')  |


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