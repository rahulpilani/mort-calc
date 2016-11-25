var svg = d3.select("svg"),
    margin = {top: 20, right: 20, bottom: 200, left: 40},
    width = +svg.attr("width") - margin.left - margin.right,
    height = +svg.attr("height") - margin.top - margin.bottom,
    g = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    h = svg.append("g")

var x = d3.scaleBand()
    .rangeRound([0, width])
    .padding(0.2)
    .align(0.5);

var leftY = d3.scaleLinear()
    .rangeRound([height, 0]);

var rightY = d3.scaleLinear()
    .rangeRound([height, 0]);

var line = d3.line()
    .x(function(d) { return x(d.months); })
    .y(function(d) { return rightY(d["remaining-amount"]); });

var z = d3.scaleOrdinal()
    .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

var stack = d3.stack();

g.append("g")
    .attr("class", "axis axis--x")
    .attr("transform", "translate(0," + height + ")")
    .call(d3.axisBottom(x).ticks(10));

g.append("g")
    .attr("class", "axis axis--y")
    .call(d3.axisLeft(leftY).ticks(10, "s"))
  .append("text")
    .attr("x", 2)
    .attr("y", leftY(leftY.ticks(10).pop()))
    .attr("dy", "0.35em")
    .attr("text-anchor", "start")
    .attr("fill", "#000")
    .text("$");

var legend = h.selectAll(".legend")
  .data(["principal", "interest"])
  .enter().append("g")
    .attr("class", "legend")
    .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; })
    .style("font", "10px sans-serif");

legend.append("rect")
    .attr("x", width)
    .attr("y", height + 50)
    .attr("width", 18)
    .attr("height", 18)
    .attr("fill", z);

legend.append("text")
    .attr("x", width - 6)
    .attr("y", height + 59)
    .attr("dy", ".35em")
    .attr("text-anchor", "end")
    .text(function(d) { return d; });

function redrawChart(data) {
  x.domain(data.map(function(d) { return d.months; }));
  leftY.domain([0, d3.max(data, function(d) { return d.principal; })]).nice();
  rightY.domain([0, d3.max(data, function(d) { return d["remaining-amount"]; })]).nice();
  z.domain(["principal", "interest"]);

  g.selectAll(".serie")
    .data(stack.keys(["principal", "interest"])(data))
    .enter().append("g")
      .attr("class", "serie")
      .attr("fill", function(d) { return z(d.key); })
      .selectAll("rect")
      .data(function(d) { return d; })
        .enter().append("rect")
        .attr("x", function(d) { return x(d.data.months); })
        .attr("y", function(d) { return leftY(d[1]); })
        .attr("height", function(d) { return leftY(d[0]) - leftY(d[1]); })
        .attr("width", x.bandwidth());

  g.append("g")
    .attr("class", "serie-2")
    .append("path")
      .datum(data)
      .attr("class", "line")
      .attr("d", line)

  g.selectAll(".serie-2")
    .data(data)
    .enter().append("circle")
      .attr("cx", function(d) {return x(d.months)})
      .attr("cy", function(d) {return rightY(d["remaining-amount"]);})
      .attr("r", 3.5)
      .style("fill", "#000");




  g.selectAll("g.axis--y")
    .call(d3.axisLeft(leftY).ticks(10, "s"));
  g.selectAll("g.axis--x")
    .call(d3.axisBottom(x).ticks(10));
}
