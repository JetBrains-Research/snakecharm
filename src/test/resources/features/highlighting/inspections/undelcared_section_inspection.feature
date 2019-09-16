Feature: Inspection if section isn't declared in rule

  Scenario Outline: When section isn't defined
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          <text>
      """
    And <inspection> inspection is enabled
    Then I expect inspection error on <<section>> in <<section><signature>> with message
      """ 
      Undeclared section usage: '<section>'.
      """
    When I check highlighting errors
    Examples:
      | rule_like  | text                     | section   | signature | inspection                       |
      | rule       | message: "{input}"       | input     | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: shell("{input}")    | input     | }         | SmkSLUndeclaredSectionInspection |

      | rule       | shell: "{input}"         | input     | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(input[0])     | input     | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{output}"        | output    | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(output[0])    | output    | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{params}"        | params    | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(params[0])    | params    | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{resources}"     | resources | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(resources[0]) | resources | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{threads}"       | threads   | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(threads[0])   | threads   | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{log}"           | log       | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(log[0])       | log       | [         | SmkUndeclaredSectionInspection   |
      | checkpoint | shell: "{params}"        | params    | }         | SmkSLUndeclaredSectionInspection |
      | checkpoint | run: print(params[0])    | params    | [         | SmkUndeclaredSectionInspection   |
      | checkpoint | shell: "{version}"       | version   | }         | SmkSLUndeclaredSectionInspection |
      | checkpoint | run: print(version[0])   | version   | [         | SmkUndeclaredSectionInspection   |

  Scenario Outline: No error if section is defined
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          <section>: ""
          <text>
      """
    And <inspection> inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  | text                     | section   | signature | inspection                       |
      | rule       | message: "{input}"       | input     | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: shell("{input}")    | input     | }         | SmkSLUndeclaredSectionInspection |

      | rule       | shell: "{input}"         | input     | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(input[0])     | input     | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{output}"        | output    | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(output[0])    | output    | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{params}"        | params    | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(params[0])    | params    | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{resources}"     | resources | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(resources[0]) | resources | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{threads}"       | threads   | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(threads[0])   | threads   | [         | SmkUndeclaredSectionInspection   |
      | rule       | shell: "{log}"           | log       | }         | SmkSLUndeclaredSectionInspection |
      | rule       | run: print(log[0])       | log       | [         | SmkUndeclaredSectionInspection   |
      | checkpoint | shell: "{params}"        | params    | }         | SmkSLUndeclaredSectionInspection |
      | checkpoint | run: print(params[0])    | params    | [         | SmkUndeclaredSectionInspection   |
      | checkpoint | shell: "{version}"       | version   | }         | SmkSLUndeclaredSectionInspection |
      | checkpoint | run: print(version[0])   | version   | [         | SmkUndeclaredSectionInspection   |


  Scenario Outline: No error when section name is other variable
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          run:
            <section> = 1
            <text>
      """
    And <inspection> inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    # not fully implemented
    Examples:
      | rule_like  | section   | text                 | inspection                       |
#      | rule       | input     | shell("{input}")     | SmkSLUndeclaredSectionInspection |
      | rule       | input     | print(input[0])      | SmkUndeclaredSectionInspection   |
#      | rule       | output    | shell("{output}")    | SmkSLUndeclaredSectionInspection |
      | rule       | output    | print(output[0])     | SmkUndeclaredSectionInspection   |
#      | rule       | params    | shell("{params}")    | SmkSLUndeclaredSectionInspection |
      | rule       | params    | print(params[0])     | SmkUndeclaredSectionInspection   |
#      | rule       | resources | shell("{resources}") | SmkSLUndeclaredSectionInspection |
      | rule       | resources | print(resources[0])  | SmkUndeclaredSectionInspection   |
      | rule       | threads   | shell("{threads}")   | SmkSLUndeclaredSectionInspection |
      | rule       | threads   | print(threads[0])    | SmkUndeclaredSectionInspection   |
#      | rule       | log       | shell("{log}")       | SmkSLUndeclaredSectionInspection |
      | rule       | log       | print(log[0])        | SmkUndeclaredSectionInspection   |
#      | rule       | input     | shell("{input}")     | SmkSLUndeclaredSectionInspection |
      | rule       | input     | print(input[0])      | SmkUndeclaredSectionInspection   |
#      | checkpoint | params    | shell("{params}")    | SmkSLUndeclaredSectionInspection |
      | checkpoint | params    | print(params[0])     | SmkUndeclaredSectionInspection   |
      | checkpoint | version   | shell("{version}")   | SmkSLUndeclaredSectionInspection |
      | checkpoint | version   | print(version[0])    | SmkUndeclaredSectionInspection   |
