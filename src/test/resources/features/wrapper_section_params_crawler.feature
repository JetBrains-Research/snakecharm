Feature: Wrappers params parsing for wrapper.py, wrapper.R

  # TODO: check description

  Scenario Outline: Parsing arguments from wrapper.py
    Given a snakemake project
    Given a file "wrapper.py" with text
    """
    <import>

    t1 = snakemake.<from_get>.get("foo", None)
    t2, t3 = snakemake.<from_array>[0], snakemake.<from_array>[1]
    t4, t5 = snakemake.<from_reference>.foo, snakemake.<from_reference>.bar
    """
    When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
    """
    <from_reference>:('bar', 'foo')
    <from_array>:()
    <from_get>:('foo')
    """
    Examples:
      | import                              | from_get  | from_array | from_reference |
      | from snakemake.shell import shell   | params    | output     | input          |
      | import snakemake.shell              | params    | output     | input          |
      | from snakemake.script import script | resources | message    | log            |

  Scenario Outline: Consider only supported section names from wrapper.py, ignore other api, see #311
    Given a snakemake project
    Given a file "wrapper.py" with text
    """
    log_append = snakemake.<call>(stdout=True, stderr=True, append=True)
    log_append = snakemake.<call>.foo(stdout=True, stderr=True, append=True)
    """
    When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
    """
    """
    Examples:
      | call          |
      | log_fmt_shell |
      | smth_else_foo |

  Scenario Outline: Consider only supported section names from wrapper.R ignore other api, see #311
    Given a snakemake project
    Given a file "wrapper.py" with text
    """
    log_append = snakemake@<call>(1)
    log_append = snakemake@<call>[["jar"]]
    """
    When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
    """
    """
    Examples:
      | call          |
      | log_fmt_shell |
      | smth_else_foo |

  Scenario Outline: Parsing arguments from wrapper.R
    Given a snakemake project
    Given a file "wrapper.r" with text
    """
    from snakemake@<import> import <import>

    t1 = snakemake@<field2>[["jar"]]
    t2, t3 = snakemake@<field1>[["foo"]], snakemake@<field1>[["bar"]]
    t4 = snakemake@<field3>
    """
    When Parse wrapper args for "meta.yaml" and "wrapper.r" result is:
    """
    <field1>:('bar', 'foo')
    <field2>:('jar')
    <field3>:()
    """
    Examples:
      | import | field1 | field2 | field3    |
      | shell  | input  | output | params    |
      | script | log    | params | resources |

  Scenario: Parsing arguments from meta.yaml  (style 1)
      Given a snakemake project
      Given a file "meta.yaml" with text
      """
      name: bismark2report
      description: |
        Generate graphical HTML report from Bismark reports (see https://github.com/FelixKrueger/Bismark/blob/master/bismark2report).
      authors:
        - Roman Cherniatchik
      input:
        - alignment_report: Alignment report (if not specified bismark will try to find it current directory)
        - nucleotide_report: Optional Bismark nucleotide coverage report (if not specified bismark will try to find it current directory)
        - dedup_report: Optional deduplication report (if not specified bismark will try to find it current directory)
        - splitting_report: Optional Bismark methylation extractor report (if not specified bismark will try to find it current directory)
        - mbias_report: Optional Bismark methylation extractor report (if not specified bismark will try to find it current directory)

      params:
        - skip_optional_reports: Use 'true' of 'false' to not look for optional reports not mentioned in input section (passes 'none' to bismark2report)
        - extra: Any additional args

      output:
        - html: Output HTML file path, if batch mode isn't used.
        - html_dir: Output dir path for HTML reports if batch mode is used
      """
      When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
      """
      input:('alignment_report', 'dedup_report', 'mbias_report', 'nucleotide_report', 'splitting_report')
      output:('html', 'html_dir')
      params:('extra', 'skip_optional_reports')
      """

  Scenario: Parsing arguments from meta.yaml  (style 2)
      Given a snakemake project
      Given a file "meta.yaml" with text
      """
      name: gatk3 PrintReads
      description: |
        Run gatk3 PrintReads
      authors:
        - Patrik Smeds
      input:
        - bam file
        - recalibration table
        - reference genome
      output:
        - bam file
      notes: |
        * The `java_opts` param allows for additional arguments to be passed to the java compiler, e.g. "-Xmx4G" for one, and "-Xmx4G -XX:ParallelGCThreads=10" for two options.
        * The `extra` param alllows for additional program arguments.
        * For more inforamtion see, https://software.broadinstitute.org/gatk/documentation/article?id=11050
        * Gatk3.jar is not included in the bioconda package, i.e it need to be added to the conda environment manually.
      """
    When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
    """
    input:()
    output:()
    """

  Scenario: Parsing arguments from meta.yaml and wrapper.py (example 1)
    Given a snakemake project
    Given a file "meta.yaml" with text
    """
    name: bismark2report
    input:
      - alignment_report: Alignment report (if not specified bismark will try to find it current directory)
      - nucleotide_report: Optional Bismark nucleotide coverage report (if not specified bismark will try to find it current directory)
      - dedup_report: Optional deduplication report (if not specified bismark will try to find it current directory)
      - splitting_report: Optional Bismark methylation extractor report (if not specified bismark will try to find it current directory)
      - mbias_report: Optional Bismark methylation extractor report (if not specified bismark will try to find it current directory)

    params:
      - skip_optional_reports: Use 'true' of 'false' to not look for optional reports not mentioned in input section (passes 'none' to bismark2report)
      - extra: Any additional args

    output:
      - html: Output HTML file path, if batch mode isn't used.
      - html_dir: Output dir path for HTML reports if batch mode is used
    """
    Given a file "wrapper.py" with text
    """
    xtra = snakemake.params.get("extra", "")
    cmds = ["bismark2report {extra}"]

    # output
    html_file = snakemake.output.get("html", "")
    output_dir = snakemake.output.get("html_dir", None)
    if output_dir is None:
        if html_file:
            output_dir = os.path.dirname(html_file)
    else:
        if html_file:
            raise ValueError(
                "bismark/bismark2report: Choose one: 'html=...' for a single dir or 'html_dir=...' for batch processing."
            )

    if output_dir is None:
        raise ValueError(
            "bismark/bismark2report: Output file or directory not specified. "
            "Use 'html=...' for a single dir or 'html_dir=...' for batch "
            "processing."
        )

    if output_dir:
        cmds.append("--dir {output_dir:q}")

    if html_file:
        html_file_name = os.path.basename(html_file)
        cmds.append("--output {html_file_name:q}")

    # reports
    reports = [
        "alignment_report",
        "dedup_report",
        "splitting_report",
        "mbias_report",
        "nucleotide_report",
    ]
    skip_optional_reports = answer2bool(
        snakemake.params.get("skip_optional_reports", False)
    )
    for report_name in reports:
        path = snakemake.input.get(report_name, "")
        if path:
            locals()[report_name] = path
            cmds.append("--{0} {{{1}:q}}".format(report_name, report_name))
        elif skip_optional_reports:
            cmds.append("--{0} 'none'".format(report_name))

    # log
    log = snakemake.log_fmt_shell(stdout=True, stderr=True)
    cmds.append("{log}")

    # run shell command:
    shell(" ".join(cmds))
    """

    When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
    """
    input:('alignment_report', 'dedup_report', 'mbias_report', 'nucleotide_report', 'splitting_report')
    output:('html', 'html_dir')
    params:('extra', 'skip_optional_reports')
    """

  Scenario: Parsing arguments from meta.yaml and wrapper.py (example 2)
      Given a snakemake project
      Given a file "meta.yaml" with text
      """
      input:
        - In SE mode one reads file with keay 'fq=...'
        - In PE mode two reads files with keys 'fq_1=...', 'fq_2=...'
        - bismark_indexes_dir: The path to the folder `Bisulfite_Genome` created by the Bismark_Genome_Preparation script, e.g. 'indexes/hg19/Bisulfite_Genome'
      params:
        - basename: File base name
        - extra: Any additional args
      output:
        - bam: Bam file. Output file will be renamed if differs from default `NAME_pe.bam` or `NAME_se.bam`
        - nucleotide_stats: Optional nucleotides report file. Output file will be renamed if differs from default `NAME_pe.nucleotide_stats.txt` or `NAME_se.nucleotide_stats.txt`
      """
      Given a file "wrapper.py" with text
      """
      extra = snakemake.params.get("extra", "")
      outdir = os.path.dirname(snakemake.output.bam)
      genome_indexes_dir = os.path.dirname(snakemake.input.bismark_indexes_dir)

      if not snakemake.output.get("bam", None):
          raise ValueError("bismark/bismark: Error 'bam' output file isn't specified.")
      if not snakemake.output.get("report", None):
          raise ValueError("bismark/bismark: Error 'report' output file isn't specified.")

      # basename
      if snakemake.params.get("basename", None):
          cmdline_args.append("--basename {snakemake.params.basename:q}")
          basename = snakemake.params.basename
      else:
          basename = None

      # reads input
      single_end_mode = snakemake.input.get("fq", None)
      if single_end_mode:
          cmdline_args.append("--se {snakemake.input.fq:q}")
          if basename is None:
              basename = basename_without_ext(snakemake.input.fq)
      else:
          cmdline_args.append("-1 {snakemake.input.fq_1:q} -2 {snakemake.input.fq_2:q}")
          mode_prefix = "pe"

          if basename is None:
              # default basename
              basename = basename_without_ext(snakemake.input.fq_1) + "_bismark_bt2"

      # Move outputs into proper position.
      expected_2_actual_paths = [
          (snakemake.output.bam, ""),
          (snakemake.output.report, ""),
          (snakemake.output.get("nucleotide_stats", None), ""),
      ]
      """

      When Parse wrapper args for "meta.yaml" and "wrapper.py" result is:
      """
      input:('bismark_indexes_dir', 'fq', 'fq_1', 'fq_2')
      output:('bam', 'nucleotide_stats', 'report')
      params:('basename', 'extra')
      """