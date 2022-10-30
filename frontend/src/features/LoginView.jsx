import React, { useState } from 'react';
import {Grid, TextField, FormControlLabel, CheckBox, Button, Typography, Link} from '@mui/material';
import {useRegisterUserMutation, useGetUserQuery} from './api/apiSlice'
import { connect, useDispatch } from 'react-redux'

export function LoginView() {

  const [password, setPassword] = useState("defaultpass");
  const [username, setUserName] = useState("defaultuser");
  const dispatch = useDispatch();

  const { data: userData, isFetching: isFetchingUser, isLoading: isLoadingUser, error: userError } = useGetUserQuery()
  const [registerUser, { data, error, isLoading }] = useRegisterUserMutation();
  const userData2 = userData || {name: "NOT"}
  const userError2 = userError || {status: 200, data: 'nothing'}

  return (<Grid>
    <Grid align='center'>
      <h2>Sign In</h2>
    </Grid>
    <TextField label='Username' placeholder='Enter username'
               variant="outlined" value={username}
               onChange={e => setUserName(event.target.value)} fullWidth required/>
    <TextField label='Password' placeholder='Enter password' type='password' variant="outlined" value={password} fullWidth required/>
    <Button type='submit' color='primary' variant="contained" onClick={() => registerUser({username, password})}>Sign in</Button>
    <Typography>
      <Link href="#">
        Forgot password ?
      </Link>
    </Typography>
    <Typography> Do you have an account ?
      <Link href="#">
        Sign Up
      </Link>
    </Typography>
      <div>
          <div>User token: { data }</div>
        <div>Error status: {userError2.status} {JSON.stringify(userError2.data)} </div>
          <div>User when logging in via token: {(isFetchingUser || error) ? "Not loaded yet" : userData2.name} </div>
      </div>
  </Grid>
  )
}
