Feature: Inspection if section isn't defined in rule

  Scenario Outline: When section isn't defined
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          <text>
      """
    And <inspection> inspection is enabled
    Then I expect inspection warning on <<section>> in <<section><signature>> with message
      """ 
      Undeclared section usage: '<section>'.
      """
    When I check highlighting warnings
    Examples:
      | rule_like  | text                     | section   | signature | inspection                       |
      | rule       | message: "{input}"       | input     | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: shell("{input}")    | input     | }         | SmkSLUnresolvedSectionInspection |

      | rule       | shell: "{input}"         | input     | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(input[0])     | input     | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{output}"        | output    | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(output[0])    | output    | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{params}"        | params    | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(params[0])    | params    | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{resources}"     | resources | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(resources[0]) | resources | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{threads}"       | threads   | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(threads[0])   | threads   | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{log}"           | log       | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(log[0])       | log       | [         | SmkUnresolvedSectionInspection   |
      | checkpoint | shell: "{params}"        | params    | }         | SmkSLUnresolvedSectionInspection |
      | checkpoint | run: print(params[0])    | params    | [         | SmkUnresolvedSectionInspection   |
      | checkpoint | shell: "{version}"       | version   | }         | SmkSLUnresolvedSectionInspection |
      | checkpoint | run: print(version[0])   | version   | [         | SmkUnresolvedSectionInspection   |

  Scenario Outline: No error if section is defined
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          <section>: ""
          <text>
      """
    And <inspection> inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
      | rule_like  | text                     | section   | signature | inspection                       |
      | rule       | message: "{input}"       | input     | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: shell("{input}")    | input     | }         | SmkSLUnresolvedSectionInspection |

      | rule       | shell: "{input}"         | input     | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(input[0])     | input     | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{output}"        | output    | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(output[0])    | output    | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{params}"        | params    | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(params[0])    | params    | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{resources}"     | resources | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(resources[0]) | resources | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{threads}"       | threads   | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(threads[0])   | threads   | [         | SmkUnresolvedSectionInspection   |
      | rule       | shell: "{log}"           | log       | }         | SmkSLUnresolvedSectionInspection |
      | rule       | run: print(log[0])       | log       | [         | SmkUnresolvedSectionInspection   |
      | checkpoint | shell: "{params}"        | params    | }         | SmkSLUnresolvedSectionInspection |
      | checkpoint | run: print(params[0])    | params    | [         | SmkUnresolvedSectionInspection   |
      | checkpoint | shell: "{version}"       | version   | }         | SmkSLUnresolvedSectionInspection |
      | checkpoint | run: print(version[0])   | version   | [         | SmkUnresolvedSectionInspection   |


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
    Then I expect no inspection warning
    When I check highlighting warnings
    # not fully implemented
    Examples:
      | rule_like  | section   | text                 | inspection                       |
#      | rule       | input     | shell("{input}")     | SmkSLUnresolvedSectionInspection |
      | rule       | input     | print(input[0])      | SmkUnresolvedSectionInspection   |
#      | rule       | output    | shell("{output}")    | SmkSLUnresolvedSectionInspection |
      | rule       | output    | print(output[0])     | SmkUnresolvedSectionInspection   |
#      | rule       | params    | shell("{params}")    | SmkSLUnresolvedSectionInspection |
      | rule       | params    | print(params[0])     | SmkUnresolvedSectionInspection   |
#      | rule       | resources | shell("{resources}") | SmkSLUnresolvedSectionInspection |
      | rule       | resources | print(resources[0])  | SmkUnresolvedSectionInspection   |
      | rule       | threads   | shell("{threads}")   | SmkSLUnresolvedSectionInspection |
      | rule       | threads   | print(threads[0])    | SmkUnresolvedSectionInspection   |
#      | rule       | log       | shell("{log}")       | SmkSLUnresolvedSectionInspection |
      | rule       | log       | print(log[0])        | SmkUnresolvedSectionInspection   |
#      | rule       | input     | shell("{input}")     | SmkSLUnresolvedSectionInspection |
      | rule       | input     | print(input[0])      | SmkUnresolvedSectionInspection   |
#      | checkpoint | params    | shell("{params}")    | SmkSLUnresolvedSectionInspection |
      | checkpoint | params    | print(params[0])     | SmkUnresolvedSectionInspection   |
      | checkpoint | version   | shell("{version}")   | SmkSLUnresolvedSectionInspection |
      | checkpoint | version   | print(version[0])    | SmkUnresolvedSectionInspection   |
