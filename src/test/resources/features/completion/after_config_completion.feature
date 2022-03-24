Feature: Completion after config[

  Scenario Outline: Completion for top-level keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
    """
    Given add key-value pairs to settings panel
      | custom_explicit_key_1 | custom_explicit_value_1 |
      | custom_explicit_key_2 | custom_explicit_value_2 |
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config[<definition><text><definition>])

    rule B:
      shell: "{config[<definition><text><definition>]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key1<result>           |
      | <result>custom_key2<result>           |
      | <result>custom_explicit_key_1<result> |
      | <result>custom_explicit_key_2<result> |
    Then completion list shouldn't contain:
      | <result>custom_key3<result>             |
      | <result>custom_explicit_value_1<result> |
      | <result>custom_explicit_value_2<result> |
    Examples:
      | definition | result | case          | text |
      |            | '      | (config[cust  | cust |
      |            | '      | (config[      |      |
      | '          |        | (config['cust | cust |
      | '          |        | (config['     |      |
      |            |        | {config[cust  | cust |
      |            |        | {config[      |      |

  Scenario Outline: Completion for top-level keys, which were added via settings panel
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
    """
    Given add file "config.yaml" as configuration file to settings panel and enable file
    Given I open a file "foo.smk" with text
    """
    rule A:
      run:
        print(config[<definition><text><definition>])

    rule B:
      shell: "{config[<definition><text><definition>]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
    Then completion list shouldn't contain:
      | <result>custom_key3<result> |
    Examples:
      | definition | result | case          | text |
      |            | '      | (config[cust  | cust |
      |            | '      | (config[      |      |
      | '          |        | (config['cust | cust |
      | '          |        | (config['     |      |
      |            |        | {config[cust  | cust |
      |            |        | {config[      |      |

  Scenario Outline: No completion for keys, which were added via settings panel, if file is disabled
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5:
            custom_key6: value4
            custom_key7: value4
    """
    Given add file "config.yaml" as configuration file to settings panel and disable file
    Given I open a file "foo.smk" with text
    """
    rule A:
      run:
        print(config<path>)
    """
    When I put the caret after <caret>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
      | <result>custom_key3<result> |
      | <result>custom_key4<result> |
      | <result>custom_key5<result> |
      | <result>custom_key6<result> |
      | <result>custom_key7<result> |
    Examples:
      | path                                                 | caret |
      | ['']                                                 | ['    |
      | ['custom_key2']['']                                  | ]['   |
      | ['custom_key2']['custom_key3'][0]['']                | 0]['  |
      | ['custom_key2']['custom_key3'][1]['custom_key5'][''] | 5'][' |

  Scenario Outline: Completion for non top-level keys mapping
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
      custom_key4:
        value2
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2'][<definition><text><definition>])

    rule B:
      shell: "{config[custom_key2][<definition><text><definition>]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key3<result> |
      | <result>custom_key4<result> |
    Then completion list shouldn't contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
    Examples:
      | definition | result | case     | text |
      |            | '      | '][cust  | cust |
      |            | '      | '][      |      |
      | '          |        | ']['cust | cust |
      | '          |        | ']['     |      |
      |            |        | 2][cust  | cust |
      |            |        | 2][      |      |

  Scenario Outline: Completion for non top-level keys mapping, which were added via settings panel
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value1
      custom_key4:
        value2
    """
    Given add file "config.yaml" as configuration file to settings panel and enable file
    Given I open a file "foo.smk" with text
    """
    rule A:
      run:
        print(config['custom_key2'][<definition><text><definition>])

    rule B:
      shell: "{config[custom_key2][<definition><text><definition>]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key3<result> |
      | <result>custom_key4<result> |
    Then completion list shouldn't contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
    Examples:
      | definition | result | case     | text |
      |            | '      | '][cust  | cust |
      |            | '      | '][      |      |
      | '          |        | ']['cust | cust |
      | '          |        | ']['     |      |
      |            |        | 2][cust  | cust |
      |            |        | 2][      |      |

  Scenario: Completion for non top-level keys mapping after sequence
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5: value3
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['custom_key3'][0][])
    """
    When I put the caret after 0][
    And I invoke autocompletion popup
    Then completion list should contain:
      | 'custom_key4' |
    Then completion list shouldn't contain:
      | 'custom_key1' |
      | 'custom_key2' |
      | 'custom_key3' |
      | 'custom_key5' |

  Scenario: Completion for non top-level keys mapping after sequence, with quotation marks case
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5: value3
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['custom_key3'][0][''])
    """
    When I put the caret after [0]['
    And I invoke autocompletion popup and see a text:
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['custom_key3'][0]['custom_key4'])
    """

  Scenario: Completion for non top-level keys mapping after sequence, SL case
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5: value3
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      shell: "{config[custom_key2][custom_key3][0][]}"
    """
    When I put the caret after [0][
    And I invoke autocompletion popup and see a text:
    """
    configfile: "config.yaml"

    rule A:
      shell: "{config[custom_key2][custom_key3][0][custom_key4]}"
    """

  Scenario Outline: Completion for non top-level keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        - custom_key4: value2
        - custom_key5:
            custom_key6: value4
            custom_key7: value4
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2']['custom_key3'][1]['custom_key5'][<definition><definition>])

    rule B:
      shell: "{config[custom_key2][custom_key3][1][custom_key5][]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key6<result> |
      | <result>custom_key7<result> |
    Then completion list shouldn't contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
      | <result>custom_key3<result> |
      | <result>custom_key4<result> |
      | <result>custom_key5<result> |
    Examples:
      | definition | result | case  |
      |            | '      | 5'][  |
      | '          |        | 5'][' |
      |            |        | 5][   |

  Scenario Outline: Completion for yaml alias
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: &keys_1
      custom_key3:
        value1
      custom_key4:
        value2
    custom_key2: *keys_1
    """
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config['custom_key2'][<definition><text><definition>])

    rule B:
      shell: "{config[custom_key2][<definition><text><definition>]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key3<result> |
      | <result>custom_key4<result> |
    Then completion list shouldn't contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
    Examples:
      | definition | result | case     | text |
      |            | '      | '][cust  | cust |
      |            | '      | '][      |      |
      | '          |        | ']['cust | cust |
      | '          |        | ']['     |      |
      |            |        | 2][cust  | cust |
      |            |        | 2][      |      |