rule all:
    input:
        expand("plots/cycle-scores.{covariate}.pdf",
               covariate=cells.columns[1:]),
        expand(["tables/diffexp.{test}.tsv",
                "plots/diffexp.{test}.bcv.pdf",
                "plots/diffexp.{test}.md.pdf",
                "plots/diffexp.{test}.disp.pdf"],
               test=config["diffexp"])