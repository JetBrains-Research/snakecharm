Feature: Inspection section argument index is out of bounds
  Scenario Outline: Out of bounds cases for section with 1 arg (in smksl)
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
          <section>:
             ""
          message: "{<section>[<idx>]}" #1
          run:
              shell("{<section>[<idx>]}") #3

      """
    And SmkSLSubscriptionIndexOutOfBoundsInspection inspection is enabled
    Then I expect inspection error on <<idx>> in <[<idx>]}" #1> with message
      """
      Section index is out of bounds. Only 0 is allowed here.
      """
    Then I expect inspection error on <<idx>> in <[<idx>]}") #3> with message
      """
      Section index is out of bounds. Only 0 is allowed here.
      """
    When I check highlighting errors
    Examples:
      | rule_like  | idx | section |
      | rule       | -1  | input   |
      | rule       | 1   | output  |
      | rule       | 2   | output  |
      | checkpoint | -1  | input   |

  Scenario Outline: Out of bounds cases for section with 1 arg (in python)
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
          <section>:
             ""
          run:
              print(<section>[<idx>])
      """
    And SmkSubscriptionIndexOutOfBoundsInspection inspection is enabled
    Then I expect inspection error on <<idx>> in <[<idx>])> with message
      """
      Section index is out of bounds. Only 0 is allowed here.
      """
    When I check highlighting errors
    Examples:
      | rule_like  | idx | section |
      | rule       | 1   | output  |
      | checkpoint | 2  | input   |

  Scenario Outline: Out of bounds cases for section with multiple arg (in smksl)
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
          <section>:
             "", "", k = 2
          message: "{<section>[<idx>]}" #1
          run:
              shell("{<section>[<idx>]}") #3

      """
    And SmkSLSubscriptionIndexOutOfBoundsInspection inspection is enabled
    Then I expect inspection error on <<idx>> in <[<idx>]}" #1> with message
      """
      Section index is out of bounds: value should be in 0..2.
      """
    Then I expect inspection error on <<idx>> in <[<idx>]}") #3> with message
      """
      Section index is out of bounds: value should be in 0..2.
      """
    When I check highlighting errors
    Examples:
      | rule_like  | idx | section |
      | rule       | -1  | input   |
      | rule       | 3   | output  |
      | rule       | 4   | output  |
      | checkpoint | -1  | input   |

  Scenario Outline: No out of bounds error
      Given a snakemake project
      Given I open a file "foo.smk" with text
        """
      <rule_like> foo:
          input:
             ""
          output:
              "", "", k = 2
          message:
              "{input[0]}"
              "{output[0]} {output[1]} {output[2]}"
          run:
            shell("{input[0]}")
            shell("{output[0]}")
            shell("{output[2]}")
            print(input[0])
            print(output[-1]) # '-' not supported
            print(output[0])
            print(output[1])
            print(output[2])
        """
      And SmkSLSubscriptionIndexOutOfBoundsInspection inspection is enabled
      And SmkSubscriptionIndexOutOfBoundsInspection inspection is enabled
      Then I expect no inspection errors
      When I check highlighting errors
      Examples:
        | rule_like  |
        | rule       |
        | checkpoint |
