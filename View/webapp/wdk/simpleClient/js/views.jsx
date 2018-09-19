
//**************************************************
// View-Controller (wraps primary page component)
//**************************************************

var ViewController = React.createClass({
  // get state from store for initial render
  getInitialState: function() {
    return this.props.store.get();
  },
  // register with store to receive update notifications
  componentDidMount: function() {
    this.props.store.register(this.update);
  },
  // handle update notifications from the store
  update: function(store) {
    this.replaceState(store.get());
  },
  // render wraps the top-level component, passing latest state
  render: function() {
    return ( <SearchPage data={this.state} ac={this.props.ac}/> );
  }
});

//**************************************************
// Primary Rendering Components
//**************************************************

var QuestionSelect = React.createClass({
  render: function() {
    return (
      <div>
        <label>Question:</label>
        <select value={this.props.selectedQuestion.name} onChange={this.props.onChange}>
          <option key={Store.NO_QUESTION_SELECTED} value={Store.NO_QUESTION_SELECTED}>Select a Search</option> );
          {this.props.questions.map(function(question) {
            return ( <option key={question} value={question}>{question}</option> );
          })}
        </select>
      </div>
    );
  }
});

function StepResults(props) {
  return <div>Created step with ID: {props.results.id}</div>;
}

function TempResultResults(props) {
  return <div><a href={props.results}>{props.results}</a></div>;
}

var SearchPage = React.createClass({
  changeQuestion: function(event) {
    this.props.ac.setQuestion(event.target.value);
  },
  render: function() {
    var midDivStyle = {
      "width": "45%",
      "display": "inline-block",
      "border": "2px solid blue",
      "borderRadius": "10px",
      "padding": "5px",
      "margin": "10px",
      "verticalAlign": "top",
      "height": "250px",
      "overflow": "scroll"
    };
    var store = this.props.data;
    var loadingStyle = ( !store.isLoading ? {"display":"none"} :
      {"height":"50px","float":"right","marginRight":"60px"} );
    var getResultPane = function(store) {
      switch(store.type) {
        case Store.ANSWER_RESULT:
          return <AnswerResults results={store.results} resultStats={store.resultStats}/>;
        case Store.STEP_RESULT:
          return <StepResults results={store.results}/>;
        case Store.TEMP_RESULT:
          return <TempResultResults results={store.results}/>;
        default:
          return <span/>;
      }
    };
    return (
      <div>
        <h3>Choose a Search<img style={loadingStyle} src="images/loading.gif"/></h3>
        <QuestionSelect questions={store.questions} selectedQuestion={store.selectedQuestion} onChange={this.changeQuestion}/>
        <div>
          <div style={midDivStyle}>
            <QuestionForm data={store} ac={this.props.ac}/>
          </div>
          <div style={midDivStyle}>
            <strong>To run this search programmatically...</strong><br/>
            <span style={{"fontSize":"0.8em"}}>
              POST the JSON below to:<br/>
              {ServiceUrl}/answer
            </span>
            <hr/>
            <QuestionJson data={store}/>
          </div>
        </div>
        {getResultPane(store)}
      </div>
    );
  }
});

var QuestionForm = React.createClass({
  changeParamValue: function(event) {
    var paramName = jQuery(event.target).data("name");
    this.props.ac.setParamValue(paramName, event.target.value);
  },
  tryToSetPaging: function(newPageNum, newPageSize) {
    // check to ensure integers
    if ((newPageNum != "" && !Util.isPositiveInteger(newPageNum)) ||
        (newPageSize != "" && !Util.isPositiveInteger(newPageSize))) {
      alert("You can only type positive integers in this field");
    }
    else {
      newPageNum = (newPageNum == "" ? null : parseInt(newPageNum));
      newPageSize = (newPageSize == "" ? null : parseInt(newPageSize));
      this.props.ac.setPagination({ pageNum: newPageNum, pageSize: newPageSize });
    }
  },
  changePageNum: function(event) {
    this.tryToSetPaging(event.target.value, this.props.data.pagination.pageSize);
  },
  changePageSize: function(event) {
    this.tryToSetPaging(this.props.data.pagination.pageNum, event.target.value);
  },
  toggleAttributePane: function(event) {
    this.props.ac.setAttributesVisible(!this.props.data.showAttributes);
  },
  toggleAttribute: function(attrName) {
    var newAttrList = Util.toggleArrayItem(this.props.data.selectedAttributes, attrName);
    this.props.ac.setSelectedAttributes(newAttrList);
  },
  submitRequest: function() {
    var store = this.props.data;
    if (store.pagination.pageNum == null || store.pagination.pageSize == null) {
      alert("You must fill in values for 'Page Size' and 'Page to Display'");
    }
    else {
      this.props.ac.loadResults(store);
    }
  },
  createStep: function() {
    var store = this.props.data;
    this.props.ac.createStep(store);
  },
  createTempResult: function() {
    var store = this.props.data;
    this.props.ac.createTempResult(store);
  },
  render: function() {
    var store = this.props.data;
    var changeParamFunction = this.changeParamValue;
    if (store.selectedQuestion.name == Store.NO_QUESTION_SELECTED) {
      // don't display anything if no question selected
      return ( <div/> );
    }
    var pageNum = store.pagination.pageNum;
    var pageSize = store.pagination.pageSize;
    if (pageNum == null) pageNum = "";
    if (pageSize == null) pageSize = "";
    return (
      <div>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Current Value</th>
            </tr>
          </thead>
          <tbody>
            {store.paramOrdering.map(function(paramName) {
              var param = store.paramValues[paramName];
              return (
                <tr key={param.name}>
                  <td>{param.displayName}:</td>
                  <td><input type="text" data-name={param.name} value={param.value} onChange={changeParamFunction}/></td>
                </tr>
              );
            })}
          </tbody>
        </table>
        <hr/>
        <div>
          <table>
            <tbody>
              <tr>
                <td>Page To Display:</td>
                <td><input type="text" value={pageNum} onChange={this.changePageNum}/></td>
              </tr>
              <tr>
                <td>Page Size:</td>
                <td><input type="text" value={pageSize} onChange={this.changePageSize}/></td>
              </tr>
            </tbody>
          </table>
        </div>
        <hr/>
        <div>
          <label>
            Select Attributes (
            <span style={{textDecoration:'underline',color:'blue',cursor:'pointer'}} onClick={this.toggleAttributePane}>
              {store.showAttributes ? "hide" : "show"}
            </span>
          )</label><br/>
          <div style={{marginLeft:'2em', display: (store.showAttributes ? 'block' : 'none')}}>
            <AttributeCheckboxList allAttributes={store.allAttributes}
                selectedAttributes={store.selectedAttributes} onChange={this.toggleAttribute}/>
          </div>
        </div>
        <hr/>
        <input type="button" value="Submit Request" onClick={this.submitRequest}/>
        <input type="button" value="Create a Step" onClick={this.createStep}/>
        <input type="button" value="Create a Temporary Result" onClick={this.createTempResult}/>
      </div>
    );
  }
});

var AttributeCheckboxList = React.createClass({
  render: function() {
    var allAttributes = this.props.allAttributes;
    var selectedAttributes = this.props.selectedAttributes;
    var onChange = this.props.onChange;
    return (
      <div>
        {allAttributes.map(function(attr){
          var checked = (selectedAttributes.indexOf(attr.name) != -1);
          return (
            <div key={attr.name}>
              <input type="checkbox" value={attr.name} checked={checked}
                onChange={function(event){ onChange(attr.name); }}/>
              <span>{attr.displayName}</span>
            </div>
          );
        })}
      </div>
    );
  }
});

var QuestionJson = React.createClass({
  render: function() {
    var data = this.props.data;
    var formattedJson = JSON.stringify(Util.getAnswerRequestJson(
      data.selectedQuestion, data.paramValues, data.pagination, data.selectedAttributes), null, 2);
    return ( <div><pre>{formattedJson}</pre></div> );
  }
});

var HtmlDiv = React.createClass({
  render: function() {
    return ( <div style={this.props.style} dangerouslySetInnerHTML={{__html: this.props.contents}}/> );
  }
});

var AnswerResults = React.createClass({
  render: function() {
    if (this.props.results == null) {
      return ( <div></div> );
    }
    var records = this.props.results.records;
    var meta = this.props.results.meta;
    var pagination = this.props.pagination;
    var headerStyle = { "border":"1px solid blue", "backgroundColor":"peachpuff" };
    var cellStyle = { "border":"1px solid blue", "fontSize":"0.8em", "verticalAlign":"top" };
    var cellDivStyle = { "overflow":"scroll", "overflowX":"hidden", "overflowY":"auto", "height":"50px" };
    // TODO: choose a key for the records- tried record.id and got non-unique instance (unexplored)
    return (
      <div>
        <div style={{"margin":"20px 0"}}>
          <strong>
            Query returned {meta.totalCount} total records of type {meta.recordClassName}.<br/>
            Showing {records.length} records on page {this.props.resultStats.pageNum}.
          </strong>
        </div>
        <table style={{"borderCollapse":"collapse"}}>
          <thead>
            <tr>
              {meta.attributes.map(function(attrib) {
                return ( <th key={attrib.name} style={headerStyle}>{attrib}</th> ); })}
            </tr>
          </thead>
          <tbody>
            {records.map(function(record) { return (
              <tr key={record.displayName}>
                {meta.attributes.map(function(attrib) { return (
                  <td key={attrib.name} style={cellStyle}>
                    <HtmlDiv style={cellDivStyle} contents={record.attributes[attrib]}/>
                  </td>
                );})}
              </tr>
            );})}
          </tbody>
        </table>
      </div>
    );
  }
});
