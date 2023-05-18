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
      document.title = `${this.state.searchTerms} | JAM-SEARCH`;
    } else {
      document.title = 'JAM-SEARCH';
    }
    event.preventDefault();
    this.setState({SearchResultValues: []});
    const pointerToThis = this;
    var url = "http://localhost:5000";

    fetch(url,{
      method: 'POST',
      mode: 'cors',
      headers: {
        'Content-Type':'application/json'
          
      },
      body: JSON.stringify({text : this.state.searchTerms})
    })
    .then(
      function(response) {
        response.json().then(function(data) {
          pointerToThis.setState({SearchResultValues: data.urls});
        })
      }
    )
  }
  
  render() {
    let searchResults = [];
    this.state.SearchResultValues.forEach((url, i) => {
      searchResults.push(
        <div className="searchResultDiv">
          <h3><a href = {this.state.SearchResultValues[i]}></a></h3>
          <span className='link'><a href={url}>{url}</a></span>
          
        </div>
      )
    })
        

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
