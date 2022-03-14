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
    Given I open a file "foo.smk" with text
    """
    configfile: "config.yaml"

    rule A:
      run:
        print(config[<definition><definition>])

    rule B:
      shell: "{config[<definition><definition>]}"
    """
    When I put the caret after <case>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <result>custom_key1<result> |
      | <result>custom_key2<result> |
    Then completion list shouldn't contain:
      | <result>custom_key3<result> |
    Examples:
      | definition | result | case      |
      |            | '      | (config[  |
      | '          |        | (config[' |
      |            |        | {config[  |

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
        print(config['custom_key2'][<definition><definition>])

    rule B:
      shell: "{config[custom_key2][<definition><definition>]}"
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
      | definition | result | case |
      |            | '      | '][  |
      | '          |        | '][' |
      |            |        | 2][  |

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