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
    .y(function(d) { return rightY(d.remaining); });

var z = d3.scaleOrdinal()
    .range(["#98abc5", "#8a89a6", "#000", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

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

var defaultLegend = ["Principal", "Interest", "Remaining Loan"];
var legend = h.selectAll(".legend")
  .data(defaultLegend)
  .enter().append("g")
      .attr("class", "legend")
      .attr("transform", function(d, i) { return "translate(" + i*80 + ", " + height + ")"; })
      .style("font", "10px sans-serif");

legend.selectAll("text")
  .text(function(d) { return d; });

legend.append("rect")
    .attr("x", 50)
    .attr("y", 60)
    .attr("width", 18)
    .attr("height", 18)
    .attr("fill", z);

legend.append("text")
    .attr("x", 75)
    .attr("y", 69)
    .attr("dy", ".35em")
    .attr("text-anchor", "start")
    .attr("class", "legend-text")
    .text(function(d) { return d; });

legend.exit().remove();

function updateLegend(d) {
  var l = h.selectAll("text.legend-text")
    .data(d)
    .text(function (d) { return d; });
}

function mapToDollar(d) {
  var principal = formatter.format(d.data.principal);
  var interest = formatter.format(d.data.interest);
  var remaining = formatter.format(d.data.remaining);

  return [principal, interest, remaining];
}

var formatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
});

function redrawChart(data) {
  x.domain(data.map(function(d) { return d.months; }));
  console.log("rightY Domain: " + d3.max(data, function(d) { return d.remaining; }));
  console.log("leftY Domain: " + d3.max(data, function(d) { return d.principal; }));
  leftY.domain([0, d3.max(data, function(d) { return d.principal; })]);
  rightY.domain([0, d3.max(data, function(d) { return d.remaining; })]);
  z.domain(["principal", "interest"]);

  g.selectAll(".serie")
    .data(stack.keys(["principal", "interest"])(data))
    .enter().append("g")
      .attr("class", "serie")
      .attr("fill", function(d) { return z(d.key); })
      .selectAll("rect")
      .data(function(d) { return d; })
        .enter().append("rect")
        .attr("class", "chart")
        .attr("x", function(d) { return x(d.data.months); })
        .attr("y", function(d) { return leftY(d[1]); })
        .attr("height", function(d) { return leftY(d[0]) - leftY(d[1]); })
        .attr("width", x.bandwidth());

  g.selectAll(".serie")
    .data(stack.keys(["principal", "interest"])(data))
    .selectAll("rect")
    .data(function(d) { return d; })
      .on('mouseover', function(d) { updateLegend(mapToDollar(d))})
      .on('mouseout', function(d) { updateLegend(defaultLegend)});


      g.select("path.line")
          .datum(data)
          .attr("d", function(d) { return line(d); });

  g.selectAll(".serie-2")
    .data([6])
    .enter().append("g")
      .attr("class", "serie-2")
      .attr("transform", "translate(6, 0)")
      .append("path").attr("class", "line").datum(data).attr("d", line);

      g.selectAll("g.serie-2")
        .selectAll("circle")
        .data(data)
        .attr("cx", function(d) { return x(d.months)})
        .attr("cy", function(d) {return rightY(d.remaining);});

  g.selectAll("g.serie-2")
    .attr("transform", "translate(6, 0)")
    .selectAll("circle")
    .data(data)
    .enter().append("circle")
      .attr("cx", function(d) { return x(d.months)})
      .attr("cy", function(d) {return rightY(d.remaining);})
      .attr("r", 3.5)
      .style("fill", "#000");

  g.selectAll("g.axis--y")
    .call(d3.axisLeft(leftY).ticks(10, "s"));
  g.selectAll("g.axis--x")
    .call(d3.axisBottom(x).ticks(10));
}
