import React from 'react';
import './App.css';
//import { getMetadata } from './metadata.jsx';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      SearchResultValues: [],
      searchTerms: '',
      totalPages: 0
    }
  }
  ChangeSearchTerms = (event) => {
    this.setState({searchTerms: event.target.value});
  }
  UseSearchEngine = (event,page) => {
    if (this.state.searchTerms !== '') {
      document.title = `${this.state.searchTerms} | JAM-SEARCH`;
    } else {
      document.title = 'JAM-SEARCH';
    }
    event.preventDefault();
    this.setState({SearchResultValues: []});
    const pointerToThis = this;
    var url = "http://localhost:5000?page="+page;

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
          pointerToThis.setState({totalPages: data.total});
        })
      }
    )
  }
  
  

  render() {
    let searchResults = [];
    this.state.SearchResultValues.forEach( async (url, i) => {
      // const metadata = await getMetadata(url);
      searchResults.push(
        <div className="searchResultDiv">
          {/* <h3 href = {metadata.title}>{metadata.title}</h3> */}
          <span className='link'><a href={url}>{url}</a></span>
          {/* <p className='description' href = {metadata.description}>{metadata.description}</p> */}
        </div>
      )
    });   

    return (
      <div className="App">
        <h1>JAM-SEARCH</h1>
        <form action ="">
          <input type="text" value={this.state.searchTerms || ''} onChange={this.ChangeSearchTerms}/>
          <button type='submit' onClick={(e)=>{this.setState({totalPages:1});this.UseSearchEngine(e,1)}}>Search</button>
        </form>
        {searchResults}
        <section>
          {[...Array(Math.round(this.state.totalPages/10)).keys()].map((i)=>{
            return <a href="" className="page" onClick={(e)=>this.UseSearchEngine(e,i+1)}>{i+1}</a>
          })}
        </section>
      </div>
    );
  }
}
export default App;
