var svg = d3.select("svg"),
    margin = {
        top: 20,
        right: 10,
        bottom: 20,
        left: 10
    },
    width = +svg.attr("width") - margin.left - margin.right,
    height = +svg.attr("height") - margin.top - margin.bottom,
    g = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

var x = d3.scaleLinear()
    .range([0, width])
    .padding(0.2)
    .align(0.5);

function redrawLinearPieChar(data) {
  x.domain([0, d3.sum(data, function(d) {
    return d.value;
  })]);

  var cumulative = 0;
  data.forEach(function (d, i) {
    data[i].cumulative = cumulative;
    cumulative =+ d.value;
  });
  g.selectAll(".linear-pie")
    .selectAll("rect")
    .data(data)
    .enter()
    .append("rect")
    .attr("class", "linear-piechart")
    .attr("x", function(d) { return x(d.cumulative); })
    .attr("y", height)
    .attr("height", 20)
    .attr("width", function(d) {return x(d.value); })

}
