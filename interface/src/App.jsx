import React from 'react';
import './App.css';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      SearchResultValues: [],
      searchTerms: ''
    }
  }
  ChangeSearchTerms = (event) => {
    this.setState({searchTerms: event.target.value});
  }

  UseSearchEngine = (event) => {
    if (this.state.searchTerms !== '') {
      document.title = `${this.state.searchTerms} - JAM-SEARCH`;
    } else {
      document.title = 'JAM-SEARCH';
    }
    event.preventDefault();
    this.setState({SearchResultValues: []});
    const pointerToThis = this;
    var url = "https://en.wikipedia.org/w/api.php";

    var params = {
      action: 'query',
      list: 'search',
      srsearch: this.state.searchTerms,
      format: 'json'  
    };

    url = url + '?origin=*';
    Object.keys(params).forEach((key) => {
      url += "&" + key + "=" + params[key];
    });

    fetch(url)
      .then(
        function (response) {
          return response.json();
        }
      )
      .then(
        function (response) {
          // console.log(response);

          for (var key in response.query.search) {
            pointerToThis.state.SearchResultValues.push({
              queryResultPageFullURL: 'no link',
              queryResultPageID: response.query.search[key].pageid,
              queryResultPageTitle: response.query.search[key].title,
              queryResultPageSnippet: response.query.search[key].snippet,
              queryResultPageWordCount: response.query.search[key].wordcount
            });
          }
        }
      )
      .then(
        function (response) {
          for (var key2 in pointerToThis.state.SearchResultValues) {
            let page = pointerToThis.state.SearchResultValues[key2];
            let pageID = page.queryResultPageID;
            let urlForRetrievingPageURLByPageID = `https://en.wikipedia.org/w/api.php?origin=*&action=query&prop=info&pageids=${pageID}&inprop=url&format=json`;

            fetch(urlForRetrievingPageURLByPageID)
              .then(
                function (response) {
                  return response.json();
                }
              )
              .then(
                function (response) {
                  page.queryResultPageFullURL = response.query.pages[pageID].fullurl;

                  pointerToThis.forceUpdate();
                }
              )
          }
        }
      )
  }
  
  render() {
    let searchResults = [];

    for (var key3 in this.state.SearchResultValues) {
      searchResults.push(
        <div className="searchResultDiv" key={key3}>
          <h3><a href={this.state.SearchResultValues[key3].queryResultPageFullURL}>{this.state.SearchResultValues[key3].queryResultPageTitle}</a></h3>
          <span className='link'><a href={this.state.SearchResultValues[key3].queryResultPageFullURL}>{this.state.SearchResultValues[key3].queryResultPageFullURL}</a></span>
          <p className="description" dangerouslySetInnerHTML={{__html: this.state.SearchResultValues[key3].queryResultPageSnippet}}></p>
        </div>
      );
    }

    return (
      <div className="App">
        <h1>JAM-SEARCH</h1>
        <form action ="">
          <input type="text" value={this.state.searchTerms || ''} onChange={this.ChangeSearchTerms}/>
          <button type='submit' onClick={this.UseSearchEngine}>Search</button>
        </form>
        {searchResults}
      </div>
    );
  }
}
export default App;
